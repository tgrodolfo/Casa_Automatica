#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <esp_wifi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>
#include <time.h>

#include "config.h"
#include "esp_wroom_32_pins.h"
#include "wifi_local.h"

#ifndef FILA_ENTRADAS_MAX
#define FILA_ENTRADAS_MAX 32
#endif
#ifndef INTERVALO_SYNC_ENTRADAS_MS
#define INTERVALO_SYNC_ENTRADAS_MS 5000
#endif
#ifndef NTP_GMT_OFFSET_SEC
#define NTP_GMT_OFFSET_SEC (-3 * 3600)
#endif
#ifndef NTP_DAYLIGHT_OFFSET_SEC
#define NTP_DAYLIGHT_OFFSET_SEC 0
#endif
#ifndef PORTA_RFID_AUTO_FECHAR_MS
#define PORTA_RFID_AUTO_FECHAR_MS 5000
#endif

#ifndef ENABLE_SAIDA_AUX
#define ENABLE_SAIDA_AUX false
#endif
#ifndef PIN_SAIDA_AUX
#define PIN_SAIDA_AUX 21
#endif

#ifndef HTTP_TIMEOUT_MS
#define HTTP_TIMEOUT_MS 15000
#endif

#if ENABLE_RFID
#include <SPI.h>
#include <MFRC522.h>
#endif

static const char* HDR_API_KEY = "X-API-Key";
static const char* TAG = "[casa]";

struct ServoConfig {
  int pin;
  int anguloAberto;
  int anguloFechado;
};

struct ServoEstado {
  Servo motor;
  int anguloAtual;
  bool anexado;
};

// Cada janela logica da API e composta por um ou mais servos que abrem/fecham juntos.
struct JanelaGrupo {
  const char* nome;
  const ServoConfig* servos;
  ServoEstado* estados;
  size_t qtd;
  bool aberto;
};

static const ServoConfig SALA_SERVOS[] = {
  {PIN_SERVO_JANELA_1, ANGULO_JANELA_1_ABERTO, ANGULO_JANELA_1_FECHADO},
  {PIN_SERVO_JANELA_2, ANGULO_JANELA_2_ABERTO, ANGULO_JANELA_2_FECHADO},
};

static const ServoConfig GARAGEM_SERVOS[] = {
  {PIN_SERVO_GARAGEM_1, ANGULO_GARAGEM_1_ABERTO, ANGULO_GARAGEM_1_FECHADO},
  {PIN_SERVO_GARAGEM_2, ANGULO_GARAGEM_2_ABERTO, ANGULO_GARAGEM_2_FECHADO},
};

static const size_t QTD_SALA_SERVOS = sizeof(SALA_SERVOS) / sizeof(SALA_SERVOS[0]);
static const size_t QTD_GARAGEM_SERVOS = sizeof(GARAGEM_SERVOS) / sizeof(GARAGEM_SERVOS[0]);

ServoEstado salaEstados[QTD_SALA_SERVOS];
ServoEstado garagemEstados[QTD_GARAGEM_SERVOS];

JanelaGrupo JANELAS_GRUPOS[] = {
  {JANELA_SALA_NOME, SALA_SERVOS, salaEstados, QTD_SALA_SERVOS, false},
  {JANELA_GARAGEM_NOME, GARAGEM_SERVOS, garagemEstados, QTD_GARAGEM_SERVOS, false},
};

static const size_t QTD_JANELAS = sizeof(JANELAS_GRUPOS) / sizeof(JANELAS_GRUPOS[0]);

Servo servoPortao;
Servo servoPortaRfid;
bool portaRfidAberta = false;
int anguloPortaRfidAtual = ANGULO_PORTA_RFID_FECHADO;
bool portaRfidAnexada = false;
bool portaoAnexado = false;
unsigned long portaRfidAbrirEm = 0;

WiFiClientSecure secureClient;
bool httpsPronto = false;
bool portaoAberto = false;
bool portaoEmMovimento = false;
int anguloPortaoAtual = ANGULO_PORTAO_FECHADO;
bool ultimaChuva = false;
unsigned long ultimoEnvioChuva = 0;
unsigned long ultimaSyncPortao = 0;
unsigned long ultimaSyncJanelas = 0;
unsigned long ultimaVerificacaoWifi = 0;
unsigned long ultimaSyncEntradas = 0;
static unsigned long proximaTentativaWifi = 0;
static const unsigned long WIFI_BACKOFF_AUTH_MS = 60000;
static volatile bool wifiAbortarTentativa = false;
static bool ntpConfigurado = false;

struct EntradaFilaItem {
  char uid[17];
  char ocorridoEm[20];
  bool offline;
};

static EntradaFilaItem filaEntradas[FILA_ENTRADAS_MAX];
static int filaEntradasQtd = 0;

#if ENABLE_RFID
MFRC522 leitorRfid(PIN_RFID_SS, PIN_RFID_RST);
#endif

void logLinha(const char* msg) {
  Serial.print(TAG);
  Serial.print(' ');
  Serial.println(msg);
}

void logFmt(const char* fmt, int v) {
  Serial.print(TAG);
  Serial.print(' ');
  Serial.printf(fmt, v);
  Serial.println();
}

bool pinPermiteSaida(int pin) {
  return wroomPinPermiteSaida(pin);
}

bool pinOkServo(int pin) {
  return wroomPinOkServo(pin);
}

void avisarPino(int pin, const char* uso) {
  if (wroomPinFlashInterna(pin)) {
    Serial.print(TAG);
    Serial.printf(" ERRO: GPIO %d (%s) e flash interna no WROOM-32\n", pin, uso);
  } else if (wroomPinSomenteEntrada(pin)) {
    Serial.print(TAG);
    Serial.printf(" ERRO: GPIO %d (%s) e somente entrada no WROOM-32\n", pin, uso);
  } else if (wroomPinStrapping(pin)) {
    Serial.print(TAG);
    Serial.printf(" AVISO: GPIO %d (%s) e pino de boot/strapping\n", pin, uso);
  } else if (wroomPinUartUsb(pin)) {
    Serial.print(TAG);
    Serial.printf(" AVISO: GPIO %d (%s) e UART USB\n", pin, uso);
  }
}

bool anexarServo(Servo& motor, int pin, bool& flag) {
  avisarPino(pin, "servo");
  if (!pinOkServo(pin)) {
    logFmt("ERRO: GPIO %d invalido para servo no WROOM-32", pin);
    flag = false;
    return false;
  }
  motor.setPeriodHertz(50);
  motor.attach(pin);
  flag = true;
  return true;
}

String normalizarNome(const String& nome) {
  String n = nome;
  n.trim();
  n.toLowerCase();
  n.replace("ó", "o");
  n.replace("ã", "a");
  n.replace("á", "a");
  n.replace("é", "e");
  return n;
}

bool usaHttps() {
  return strncmp(API_BASE_URL, "https://", 8) == 0;
}

void prepararHttps() {
  if (!usaHttps() || httpsPronto) {
    return;
  }
  secureClient.setInsecure();
  secureClient.setHandshakeTimeout(HTTP_TIMEOUT_MS / 1000);
  httpsPronto = true;
}

void configurarHttp(HTTPClient& http) {
  http.setTimeout(HTTP_TIMEOUT_MS);
  http.setReuse(false);
}

bool iniciarHttp(HTTPClient& http, const char* path) {
  configurarHttp(http);
  String url = String(API_BASE_URL) + path;
  if (usaHttps()) {
    prepararHttps();
    if (!http.begin(secureClient, url)) {
      logLinha("ERRO: http.begin HTTPS falhou");
      return false;
    }
    return true;
  }
  return http.begin(url);
}

void encerrarHttp(HTTPClient& http) {
  http.end();
  if (usaHttps()) {
    secureClient.stop();
    delay(1);
  }
}

#ifndef WIFI_CONNECT_TIMEOUT_MS
#define WIFI_CONNECT_TIMEOUT_MS 45000
#endif

bool wifiConectado() {
  return WiFi.status() == WL_CONNECTED;
}

const char* wifiStatusText(wl_status_t status) {
  switch (status) {
    case WL_IDLE_STATUS: return "idle";
    case WL_NO_SSID_AVAIL: return "rede nao encontrada";
    case WL_SCAN_COMPLETED: return "scan ok";
    case WL_CONNECTED: return "conectado";
    case WL_CONNECT_FAILED: return "senha incorreta ou falha";
    case WL_CONNECTION_LOST: return "conexao perdida";
    case WL_DISCONNECTED: return "desconectado";
    default: return "desconhecido";
  }
}

const char* wifiMotivoText(uint8_t reason) {
  switch (reason) {
    case WIFI_REASON_UNSPECIFIED: return "indefinido";
    case WIFI_REASON_AUTH_EXPIRE: return "falha autenticacao (senha/WPA3)";
    case WIFI_REASON_AUTH_LEAVE: return "auth saiu";
    case WIFI_REASON_ASSOC_EXPIRE: return "assoc expirou";
    case WIFI_REASON_ASSOC_TOOMANY: return "muitos clientes";
    case WIFI_REASON_NOT_AUTHED: return "nao autenticado";
    case WIFI_REASON_NOT_ASSOCED: return "nao associado";
    case WIFI_REASON_ASSOC_LEAVE: return "saiu da rede";
    case WIFI_REASON_ASSOC_NOT_AUTHED: return "assoc sem auth";
    case WIFI_REASON_DISASSOC_PWRCAP_BAD: return "energia insuficiente AP";
    case WIFI_REASON_DISASSOC_SUPCHAN_BAD: return "canal ruim";
    case WIFI_REASON_IE_INVALID: return "IE invalido";
    case WIFI_REASON_MIC_FAILURE: return "falha MIC";
    case WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT: return "timeout handshake (senha?)";
    case WIFI_REASON_GROUP_KEY_UPDATE_TIMEOUT: return "timeout chave grupo";
    case WIFI_REASON_IE_IN_4WAY_DIFFERS: return "IE diferente";
    case WIFI_REASON_GROUP_CIPHER_INVALID: return "cipher grupo invalido";
    case WIFI_REASON_PAIRWISE_CIPHER_INVALID: return "cipher par invalido";
    case WIFI_REASON_AKMP_INVALID: return "AKMP invalido";
    case WIFI_REASON_UNSUPP_RSN_IE_VERSION: return "RSN IE invalido";
    case WIFI_REASON_INVALID_RSN_IE_CAP: return "RSN cap invalido";
    case WIFI_REASON_802_1X_AUTH_FAILED: return "802.1X falhou";
    case WIFI_REASON_CIPHER_SUITE_REJECTED: return "cipher rejeitado";
    case WIFI_REASON_BEACON_TIMEOUT: return "beacon timeout";
    case WIFI_REASON_NO_AP_FOUND: return "AP nao encontrado";
    case WIFI_REASON_AUTH_FAIL: return "senha incorreta";
    case WIFI_REASON_ASSOC_FAIL: return "falha associacao";
    case WIFI_REASON_HANDSHAKE_TIMEOUT: return "timeout handshake";
    case WIFI_REASON_CONNECTION_FAIL: return "falha conexao";
    default: return "outro";
  }
}

void logMacWifi() {
  Serial.print(TAG);
  Serial.print(" wifi: MAC ");
  Serial.println(WiFi.macAddress());
}

void logWifiOk() {
  Serial.print(TAG);
  Serial.print(" wifi: ok IP ");
  Serial.print(WiFi.localIP());
  Serial.print(" RSSI ");
  Serial.print(WiFi.RSSI());
  Serial.println(" dBm");
}

void logWifiFalha() {
  wl_status_t status = WiFi.status();
  Serial.print(TAG);
  Serial.print(" wifi: falhou (");
  Serial.print(wifiStatusText(status));
  Serial.print(" code=");
  Serial.print(status);
  Serial.print(") SSID=");
  Serial.println(WIFI_SSID);
}

bool ssidIgual(const char* alvo, const String& encontrado) {
  return encontrado.equalsIgnoreCase(alvo);
}

const char* wifiAuthModeText(wifi_auth_mode_t modo) {
  switch (modo) {
    case WIFI_AUTH_OPEN: return "aberta";
    case WIFI_AUTH_WEP: return "WEP";
    case WIFI_AUTH_WPA_PSK: return "WPA";
    case WIFI_AUTH_WPA2_PSK: return "WPA2";
    case WIFI_AUTH_WPA_WPA2_PSK: return "WPA/WPA2";
    case WIFI_AUTH_WPA2_ENTERPRISE: return "WPA2 Enterprise";
    case WIFI_AUTH_WPA3_PSK: return "WPA3";
    case WIFI_AUTH_WPA2_WPA3_PSK: return "WPA2+WPA3";
    default: return "outro";
  }
}

bool motivoFalhaAutenticacao(uint8_t reason) {
  return reason == WIFI_REASON_AUTH_EXPIRE
      || reason == WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT
      || reason == WIFI_REASON_AUTH_FAIL
      || reason == WIFI_REASON_HANDSHAKE_TIMEOUT
      || reason == WIFI_REASON_MIC_FAILURE;
}

void prepararModoWifi() {
  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  delay(300);
  WiFi.setAutoReconnect(false);
  WiFi.setSleep(false);
  WiFi.setTxPower(WIFI_POWER_19_5dBm);
  esp_wifi_set_ps(WIFI_PS_NONE);
}

void logBssid(const uint8_t* bssid) {
  Serial.print(TAG);
  Serial.print(" wifi: BSSID ");
  for (int i = 0; i < 6; i++) {
    if (i > 0) {
      Serial.print(':');
    }
    if (bssid[i] < 16) {
      Serial.print('0');
    }
    Serial.print(bssid[i], HEX);
  }
  Serial.println();
}

bool aguardarIp(unsigned long timeoutMs) {
  unsigned long inicio = millis();
  while (millis() - inicio < timeoutMs) {
    if (wifiAbortarTentativa) {
      return false;
    }
    if (WiFi.status() == WL_CONNECTED && WiFi.localIP()[0] != 0) {
      return true;
    }
    delay(200);
    yield();
  }
  return WiFi.status() == WL_CONNECTED && WiFi.localIP()[0] != 0;
}

bool tentarConexao(const char* rotulo, int canal, const uint8_t* bssid, bool usarBssid) {
  wifiAbortarTentativa = false;
  prepararModoWifi();
  WiFi.disconnect(false);
  delay(150);

  Serial.print(TAG);
  Serial.print(" wifi: tentativa ");
  Serial.println(rotulo);

  WiFi.setMinSecurity(WIFI_AUTH_WPA_PSK);

  if (usarBssid && bssid != nullptr) {
    logBssid(bssid);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD, canal, bssid);
  } else {
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  }

  if (aguardarIp(20000)) {
    return true;
  }

  if (wifiAbortarTentativa) {
    logLinha("wifi: auth falhou nesta tentativa");
  } else {
    logLinha("wifi: timeout nesta tentativa");
  }
  WiFi.disconnect(false);
  delay(300);
  return false;
}

int coletarCandidatos(CandidatoAp* candidatos, int maxCandidatos) {
  int encontrados = 0;
  int total = WiFi.scanComplete();
  if (total <= 0) {
    return 0;
  }

  for (int i = 0; i < total && encontrados < maxCandidatos; i++) {
    if (!ssidIgual(WIFI_SSID, WiFi.SSID(i))) {
      continue;
    }
    candidatos[encontrados].canal = WiFi.channel(i);
    candidatos[encontrados].rssi = WiFi.RSSI(i);
    candidatos[encontrados].modo = static_cast<wifi_auth_mode_t>(WiFi.encryptionType(i));
    memcpy(candidatos[encontrados].bssid, WiFi.BSSID(i), 6);
    encontrados++;
  }

  for (int i = 0; i < encontrados - 1; i++) {
    for (int j = i + 1; j < encontrados; j++) {
      if (candidatos[j].rssi > candidatos[i].rssi) {
        CandidatoAp tmp = candidatos[i];
        candidatos[i] = candidatos[j];
        candidatos[j] = tmp;
      }
    }
  }
  return encontrados;
}

int buscarRedeAlvo(int& canal, uint8_t bssid[6], int& rssi) {
  logLinha("wifi: escaneando redes...");
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(false);
  delay(200);

  int total = WiFi.scanNetworks(false, true);
  if (total <= 0) {
    logLinha("wifi: nenhuma rede no scan");
    return -1;
  }

  logFmt("wifi: %d rede(s) visiveis", total);
  CandidatoAp candidatos[8];
  int qtd = coletarCandidatos(candidatos, 8);
  if (qtd == 0) {
    logLinha("wifi: SSID configurado NAO apareceu no scan");
    return -1;
  }

  for (int i = 0; i < qtd; i++) {
    Serial.print(TAG);
    Serial.print("   ");
    Serial.print(WIFI_SSID);
    Serial.print(" ch");
    Serial.print(candidatos[i].canal);
    Serial.print(" (");
    Serial.print(candidatos[i].rssi);
    Serial.print(" dBm) ");
    Serial.println(wifiAuthModeText(candidatos[i].modo));
    if (candidatos[i].modo == WIFI_AUTH_WPA2_ENTERPRISE || candidatos[i].modo == WIFI_AUTH_WPA3_ENTERPRISE) {
      logLinha("wifi: ERRO rede Enterprise - ESP nao suporta");
      return -2;
    }
  }

  canal = candidatos[0].canal;
  rssi = candidatos[0].rssi;
  memcpy(bssid, candidatos[0].bssid, 6);
  if (candidatos[0].modo == WIFI_AUTH_WPA3_PSK || candidatos[0].modo == WIFI_AUTH_WPA2_WPA3_PSK) {
    logLinha("wifi: rede WPA2+WPA3 - celular usa WPA3, ESP pode precisar WPA2 no AP");
  }
  return qtd;
}

void onWifiEvent(WiFiEvent_t event, WiFiEventInfo_t info) {
  switch (event) {
    case ARDUINO_EVENT_WIFI_STA_CONNECTED:
      logLinha("wifi: associado ao roteador");
      break;
    case ARDUINO_EVENT_WIFI_STA_GOT_IP:
      break;
    case ARDUINO_EVENT_WIFI_STA_DISCONNECTED: {
      uint8_t reason = info.wifi_sta_disconnected.reason;
      Serial.print(TAG);
      Serial.print(" wifi: desconectado reason=");
      Serial.print(reason);
      Serial.print(" (");
      Serial.print(wifiMotivoText(reason));
      Serial.println(')');
      if (motivoFalhaAutenticacao(reason)) {
        wifiAbortarTentativa = true;
        esp_wifi_disconnect();
      }
      break;
    }
    default:
      break;
  }
}

void conectarWifi() {
  if (wifiConectado()) {
    return;
  }
  if (millis() < proximaTentativaWifi) {
    return;
  }

  wifiAbortarTentativa = false;

  logMacWifi();
  Serial.print(TAG);
  Serial.print(" wifi: conectando ");
  Serial.println(WIFI_SSID);
  logFmt("wifi: senha com %d caracteres", static_cast<int>(strlen(WIFI_PASSWORD)));

  int canal = 0;
  int rssi = 0;
  uint8_t bssid[6] = {0};
  int qtdAps = buscarRedeAlvo(canal, bssid, rssi);

  if (qtdAps == -2) {
    proximaTentativaWifi = millis() + WIFI_BACKOFF_AUTH_MS;
    return;
  }

  CandidatoAp candidatos[8];
  int candidatosScan = coletarCandidatos(candidatos, 8);
  bool conectou = false;

  if (tentarConexao("simples (sem BSSID)", 0, nullptr, false)) {
    conectou = true;
  } else if (candidatosScan > 0) {
    for (int i = 0; i < candidatosScan && !conectou; i++) {
      char rotulo[32];
      snprintf(rotulo, sizeof(rotulo), "AP %d ch%d", i + 1, candidatos[i].canal);
      if (tentarConexao(rotulo, candidatos[i].canal, candidatos[i].bssid, true)) {
        conectou = true;
      }
    }
  } else if (qtdAps < 0) {
    logLinha("wifi: SSID fora do scan, tentando simples...");
    conectou = tentarConexao("fora do scan", 0, nullptr, false);
  }

  if (conectou) {
    WiFi.setAutoReconnect(true);
    logWifiOk();
  } else {
    logWifiFalha();
    logLinha("wifi: senha pode estar certa, mas rede WPA2+WPA3 do IFSP");
    logLinha("wifi: bloqueia ESP32 - peca WPA2-PSK ou use hotspot");
    proximaTentativaWifi = millis() + WIFI_BACKOFF_AUTH_MS;
  }
}

void aplicarHeadersApi(HTTPClient& http) {
  http.addHeader("Content-Type", "application/json");
  http.addHeader(HDR_API_KEY, API_KEY);
}

void configurarNtp() {
  if (ntpConfigurado || !wifiConectado()) {
    return;
  }
  configTime(NTP_GMT_OFFSET_SEC, NTP_DAYLIGHT_OFFSET_SEC, "pool.ntp.org", "time.nist.gov");
  ntpConfigurado = true;
  logLinha("ntp: configurado (America/Sao_Paulo UTC-3)");
}

bool horaLocalIso(char* dest, size_t destLen) {
  if (dest == nullptr || destLen < 20) {
    return false;
  }
  time_t agora = time(nullptr);
  if (agora < 1700000000) {
    dest[0] = '\0';
    return false;
  }
  struct tm info;
  if (!localtime_r(&agora, &info)) {
    dest[0] = '\0';
    return false;
  }
  snprintf(
    dest,
    destLen,
    "%04d-%02d-%02dT%02d:%02d:%02d",
    info.tm_year + 1900,
    info.tm_mon + 1,
    info.tm_mday,
    info.tm_hour,
    info.tm_min,
    info.tm_sec
  );
  return true;
}

String uidCartaoHex() {
#if ENABLE_RFID
  String uid;
  for (byte i = 0; i < leitorRfid.uid.size; i++) {
    if (leitorRfid.uid.uidByte[i] < 0x10) {
      uid += '0';
    }
    uid += String(leitorRfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  return uid;
#else
  return String();
#endif
}

bool enfileirarEntrada(const String& uid, const char* ocorridoEm, bool offline) {
  if (filaEntradasQtd >= FILA_ENTRADAS_MAX) {
    logLinha("fila entradas: CHEIA, descartando a mais antiga");
    for (int i = 1; i < filaEntradasQtd; i++) {
      filaEntradas[i - 1] = filaEntradas[i];
    }
    filaEntradasQtd--;
  }

  EntradaFilaItem& item = filaEntradas[filaEntradasQtd];
  strncpy(item.uid, uid.c_str(), sizeof(item.uid) - 1);
  item.uid[sizeof(item.uid) - 1] = '\0';
  if (ocorridoEm != nullptr && ocorridoEm[0] != '\0') {
    strncpy(item.ocorridoEm, ocorridoEm, sizeof(item.ocorridoEm) - 1);
    item.ocorridoEm[sizeof(item.ocorridoEm) - 1] = '\0';
  } else {
    item.ocorridoEm[0] = '\0';
  }
  item.offline = offline;
  filaEntradasQtd++;
  logFmt("fila entradas: %d pendente(s)", filaEntradasQtd);
  return true;
}

bool postEntradaPorta(const char* uid, const char* ocorridoEm, bool offline) {
  if (!wifiConectado()) {
    return false;
  }

  HTTPClient http;
  if (!iniciarHttp(http, "/api/porta/entradas")) {
    return false;
  }

  StaticJsonDocument<192> doc;
  if (uid != nullptr && uid[0] != '\0') {
    doc["cardUid"] = uid;
  }
  if (ocorridoEm != nullptr && ocorridoEm[0] != '\0') {
    doc["ocorridoEm"] = ocorridoEm;
  }
  doc["offline"] = offline;

  String body;
  serializeJson(doc, body);
  aplicarHeadersApi(http);
  int code = http.POST(body);
  encerrarHttp(http);

  if (code == HTTP_CODE_OK || code == HTTP_CODE_CREATED) {
    logLinha(offline ? "POST /api/porta/entradas (sync offline) ok" : "POST /api/porta/entradas ok");
    return true;
  }

  logFmt("POST /api/porta/entradas falhou HTTP %d", code);
  return false;
}

void registrarEntradaPorta(const String& uid) {
  char iso[20];
  bool temHora = horaLocalIso(iso, sizeof(iso));
  const char* ocorrido = temHora ? iso : "";

  if (postEntradaPorta(uid.c_str(), ocorrido, false)) {
    return;
  }

  enfileirarEntrada(uid, ocorrido, true);
  logLinha("entrada porta: sem net, guardado na fila local");
}

void sincronizarFilaEntradas() {
  if (filaEntradasQtd == 0 || !wifiConectado()) {
    return;
  }

  configurarNtp();

  while (filaEntradasQtd > 0) {
    EntradaFilaItem& item = filaEntradas[0];
    if (!postEntradaPorta(item.uid, item.ocorridoEm, true)) {
      break;
    }
    for (int i = 1; i < filaEntradasQtd; i++) {
      filaEntradas[i - 1] = filaEntradas[i];
    }
    filaEntradasQtd--;
  }
}

bool lerChuva() {
  digitalWrite(PIN_CHUVA_POWER, LOW);
  delay(10);
  int estado = digitalRead(PIN_CHUVA_DO);
  digitalWrite(PIN_CHUVA_POWER, LOW);
  return estado == LOW;
}

void escreverAngulo(Servo& motor, int angulo, int& anguloAtual) {
  motor.write(angulo);
  anguloAtual = angulo;
}

int indiceJanelaPorNome(const String& nomeApi) {
  String alvo = normalizarNome(nomeApi);
  for (size_t i = 0; i < QTD_JANELAS; i++) {
    if (normalizarNome(JANELAS_GRUPOS[i].nome) == alvo) {
      return static_cast<int>(i);
    }
  }
  return -1;
}

void moverJanela(size_t indice, bool aberta) {
  if (indice >= QTD_JANELAS) {
    return;
  }
  JanelaGrupo& grupo = JANELAS_GRUPOS[indice];
  for (size_t i = 0; i < grupo.qtd; i++) {
    if (!grupo.estados[i].anexado) {
      continue;
    }
    int angulo = aberta ? grupo.servos[i].anguloAberto : grupo.servos[i].anguloFechado;
    escreverAngulo(grupo.estados[i].motor, angulo, grupo.estados[i].anguloAtual);
  }
  grupo.aberto = aberta;
  Serial.print(TAG);
  Serial.print(' ');
  Serial.print(grupo.nome);
  Serial.print(aberta ? " aberta " : " fechada ");
  Serial.print('(');
  Serial.print(grupo.qtd);
  Serial.println(" servos)");
}

void fecharTodasJanelas() {
  for (size_t i = 0; i < QTD_JANELAS; i++) {
    if (JANELAS_GRUPOS[i].aberto) {
      moverJanela(i, false);
    }
  }
}

void moverPortaoDevagar(bool aberto) {
  if (!portaoAnexado) {
    return;
  }

  portaoEmMovimento = true;
  int destino = aberto ? ANGULO_PORTAO_ABERTO : ANGULO_PORTAO_FECHADO;
  int passo = PORTAO_GRAUS_POR_MS;
  if (passo < 1) {
    passo = 1;
  }

  if (anguloPortaoAtual > destino) {
    for (int ang = anguloPortaoAtual; ang > destino; ang -= passo) {
      if (ang < destino) {
        ang = destino;
      }
      escreverAngulo(servoPortao, ang, anguloPortaoAtual);
      delay(1);
      yield();
    }
  } else if (anguloPortaoAtual < destino) {
    for (int ang = anguloPortaoAtual; ang < destino; ang += passo) {
      if (ang > destino) {
        ang = destino;
      }
      escreverAngulo(servoPortao, ang, anguloPortaoAtual);
      delay(1);
      yield();
    }
  }

  escreverAngulo(servoPortao, destino, anguloPortaoAtual);
  portaoAberto = aberto;
  portaoEmMovimento = false;
  logFmt(aberto ? "portao aberto devagar (%d graus)" : "portao fechado devagar (%d graus)", destino);
}

void moverServoPortao(bool aberto) {
  if (!portaoAnexado || portaoEmMovimento) {
    return;
  }

  moverPortaoDevagar(aberto);
}

void moverPortaRfid(bool aberta) {
  if (!portaRfidAnexada) {
    return;
  }
  int angulo = aberta ? ANGULO_PORTA_RFID_ABERTO : ANGULO_PORTA_RFID_FECHADO;
  escreverAngulo(servoPortaRfid, angulo, anguloPortaRfidAtual);
  portaRfidAberta = aberta;
  if (aberta) {
    portaRfidAbrirEm = millis();
    logFmt("porta rfid aberta (%d graus)", angulo);
  } else {
    portaRfidAbrirEm = 0;
    logFmt("porta rfid fechada (%d graus)", angulo);
  }
}

void processarAutoFecharPortaRfid() {
  if (!portaRfidAberta || PORTA_RFID_AUTO_FECHAR_MS == 0) {
    return;
  }
  if (millis() - portaRfidAbrirEm < PORTA_RFID_AUTO_FECHAR_MS) {
    return;
  }
  logLinha("rfid: auto-fechando porta");
  moverPortaRfid(false);
}

bool postChuva(bool chovendo) {
  if (!wifiConectado()) {
    return false;
  }

  HTTPClient http;
  if (!iniciarHttp(http, "/api/chuva")) {
    return false;
  }

  aplicarHeadersApi(http);
  const char* body = chovendo ? "{\"rain\":\"yes\"}" : "{\"rain\":\"no\"}";
  int code = http.POST(body);
  encerrarHttp(http);

  if (code == HTTP_CODE_OK || code == HTTP_CODE_CREATED) {
    logLinha(chovendo ? "POST /api/chuva -> yes" : "POST /api/chuva -> no");
    if (chovendo) {
      fecharTodasJanelas();
    }
    return true;
  }

  logFmt("POST /api/chuva falhou HTTP %d", code);
  return false;
}

bool getPortao(bool& aberto) {
  if (!wifiConectado()) {
    return false;
  }

  HTTPClient http;
  if (!iniciarHttp(http, "/api/portao")) {
    return false;
  }

  http.addHeader(HDR_API_KEY, API_KEY);
  int code = http.GET();
  if (code != HTTP_CODE_OK) {
    logFmt("GET /api/portao falhou HTTP %d", code);
    encerrarHttp(http);
    return false;
  }

  String payload = http.getString();
  encerrarHttp(http);

  StaticJsonDocument<256> doc;
  if (deserializeJson(doc, payload)) {
    logLinha("JSON invalido em /api/portao");
    return false;
  }

  const char* state = doc["state"] | "closed";
  aberto = strcmp(state, "open") == 0;
  return true;
}

bool sincronizarJanelasDaApi() {
  if (!wifiConectado()) {
    return false;
  }

  HTTPClient http;
  if (!iniciarHttp(http, "/api/janelas")) {
    return false;
  }

  http.addHeader(HDR_API_KEY, API_KEY);
  int code = http.GET();
  if (code != HTTP_CODE_OK) {
    logFmt("GET /api/janelas falhou HTTP %d", code);
    encerrarHttp(http);
    return false;
  }

  String payload = http.getString();
  encerrarHttp(http);

  StaticJsonDocument<1536> doc;
  DeserializationError err = deserializeJson(doc, payload);
  if (err) {
    Serial.print(TAG);
    Serial.print(" JSON /api/janelas: ");
    Serial.println(err.c_str());
    return false;
  }

  bool bloqueadas = doc["windowsBlocked"] | false;
  JsonArray lista = doc["windows"].as<JsonArray>();
  if (lista.isNull()) {
    logLinha("JSON /api/janelas sem array windows");
    return false;
  }

  for (JsonObject janela : lista) {
    const char* nome = janela["window"] | "";
    int idx = indiceJanelaPorNome(String(nome));
    if (idx < 0) {
      continue;
    }

    const char* state = janela["state"] | "closed";
    bool deveAbrir = strcmp(state, "open") == 0;
    if (bloqueadas) {
      deveAbrir = false;
    }

    if (deveAbrir != JANELAS_GRUPOS[idx].aberto) {
      moverJanela(static_cast<size_t>(idx), deveAbrir);
    }
  }

  return true;
}

void abrirPortaPorRfid(const String& uid) {
  registrarEntradaPorta(uid);
  if (portaRfidAberta) {
    portaRfidAbrirEm = millis();
    logLinha("rfid: porta ja aberta, renovando auto-fechar");
    return;
  }
  logLinha("rfid: cartao lido, abrindo porta");
  moverPortaRfid(true);
}

#if ENABLE_LEDS_LUZES
int pinLedPorNome(const String& nome) {
  if (nome == "sala") return PIN_LED_SALA;
  if (nome == "quarto") return PIN_LED_QUARTO;
  if (nome == "cozinha") return PIN_LED_COZINHA;
  return -1;
}

void sincronizarLuzes() {
  if (!wifiConectado()) {
    return;
  }

  HTTPClient http;
  if (!iniciarHttp(http, "/api/quartos")) {
    return;
  }

  http.addHeader(HDR_API_KEY, API_KEY);
  int code = http.GET();
  if (code != HTTP_CODE_OK) {
    encerrarHttp(http);
    return;
  }

  String payload = http.getString();
  encerrarHttp(http);

  StaticJsonDocument<1536> doc;
  if (deserializeJson(doc, payload)) {
    return;
  }

  for (JsonObject room : doc["rooms"].as<JsonArray>()) {
    const char* roomName = room["room"] | "";
    const char* light = room["light"] | "off";
    int pin = pinLedPorNome(normalizarNome(String(roomName)));
    if (pin != -1) {
      digitalWrite(pin, strcmp(light, "on") == 0 ? HIGH : LOW);
    }
  }
}
#endif

void processarChuva() {
  unsigned long agora = millis();
  if (agora - ultimoEnvioChuva < INTERVALO_CHUVA_MS) {
    return;
  }
  ultimoEnvioChuva = agora;

  bool chovendo = lerChuva();
  if (chovendo == ultimaChuva) {
    return;
  }

  if (postChuva(chovendo)) {
    ultimaChuva = chovendo;
  }
}

void sincronizarPortao() {
  if (portaoEmMovimento) {
    return;
  }

  unsigned long agora = millis();
  if (agora - ultimaSyncPortao < INTERVALO_PORTAO_MS) {
    return;
  }
  ultimaSyncPortao = agora;

  bool aberto = false;
  if (!getPortao(aberto)) {
    return;
  }

  if (aberto != portaoAberto) {
    moverServoPortao(aberto);
  }
}

void sincronizarJanelas() {
  unsigned long agora = millis();
  if (agora - ultimaSyncJanelas < INTERVALO_JANELAS_MS) {
    return;
  }
  ultimaSyncJanelas = agora;
  sincronizarJanelasDaApi();
}

void sincronizarEstadoInicial() {
  ultimaChuva = lerChuva();
  postChuva(ultimaChuva);

  bool aberto = false;
  if (getPortao(aberto)) {
    moverServoPortao(aberto);
  }

  sincronizarJanelasDaApi();
}

void configurarServos() {
  ESP32PWM::allocateTimer(0);
  ESP32PWM::allocateTimer(1);
  ESP32PWM::allocateTimer(2);
  ESP32PWM::allocateTimer(3);

  if (anexarServo(servoPortao, PIN_SERVO_PORTAO, portaoAnexado)) {
    escreverAngulo(servoPortao, ANGULO_PORTAO_FECHADO, anguloPortaoAtual);
    portaoAberto = false;
  }

  for (size_t g = 0; g < QTD_JANELAS; g++) {
    JanelaGrupo& grupo = JANELAS_GRUPOS[g];
    grupo.aberto = false;
    for (size_t i = 0; i < grupo.qtd; i++) {
      grupo.estados[i].anguloAtual = grupo.servos[i].anguloFechado;
      grupo.estados[i].anexado = false;
      anexarServo(grupo.estados[i].motor, grupo.servos[i].pin, grupo.estados[i].anexado);
    }
    moverJanela(g, false);
  }

  if (anexarServo(servoPortaRfid, PIN_SERVO_PORTA_RFID, portaRfidAnexada)) {
    escreverAngulo(servoPortaRfid, ANGULO_PORTA_RFID_FECHADO, anguloPortaRfidAtual);
    portaRfidAberta = false;
  }
}

void validarPinos() {
  logLinha("placa: ESP-WROOM-32");

  avisarPino(PIN_SERVO_PORTAO, "portao");
  avisarPino(PIN_SERVO_JANELA_1, "janela sala");
  avisarPino(PIN_SERVO_JANELA_2, "janela garagem");
  avisarPino(PIN_SERVO_GARAGEM_1, "garagem 1");
  avisarPino(PIN_SERVO_GARAGEM_2, "garagem 2");
  avisarPino(PIN_SERVO_PORTA_RFID, "porta rfid");
  avisarPino(PIN_CHUVA_POWER, "chuva POWER");
  avisarPino(PIN_CHUVA_DO, "chuva DO");
#if ENABLE_SAIDA_AUX
  avisarPino(PIN_SAIDA_AUX, "aux");
#endif

  if (!wroomPinSomenteEntrada(PIN_CHUVA_DO) && !wroomPinPermiteEntrada(PIN_CHUVA_DO)) {
    logFmt("ERRO: PIN_CHUVA_DO=%d invalido", PIN_CHUVA_DO);
  }
  if (!wroomPinPermiteSaida(PIN_CHUVA_POWER)) {
    logFmt("ERRO: PIN_CHUVA_POWER=%d nao pode ser saida", PIN_CHUVA_POWER);
  }

  if (PIN_CHUVA_POWER == PIN_RFID_SS) {
    logLinha("ERRO: chuva POWER no mesmo pino do RFID SS");
  }
#if ENABLE_LEDS_LUZES
  avisarPino(PIN_LED_COZINHA, "LED cozinha");
  avisarPino(PIN_LED_QUARTO, "LED quarto");
  avisarPino(PIN_LED_SALA, "LED sala");
  if (PIN_LED_SALA == PIN_RFID_SS) {
    logLinha("ERRO: LED sala no GPIO 5 (RFID SS)");
  }
#endif
#if ENABLE_SAIDA_AUX
  if (PIN_SAIDA_AUX == PIN_RFID_MOSI) {
    logLinha("ERRO: aux no GPIO 23 (SPI MOSI)");
  }
#endif
}

#if ENABLE_RFID
void configurarRfid() {
  SPI.begin(PIN_RFID_SCK, PIN_RFID_MISO, PIN_RFID_MOSI, PIN_RFID_SS);
  leitorRfid.PCD_Init();
  byte versao = leitorRfid.PCD_ReadRegister(leitorRfid.VersionReg);
  if (versao == 0x00 || versao == 0xFF) {
    logLinha("ERRO: RFID nao detectado (verifique fios SPI/SS/RST)");
  } else {
    logFmt("rfid: ok versao=0x%02X", versao);
  }
}

void processarRfid() {
  if (!leitorRfid.PICC_IsNewCardPresent()) {
    return;
  }
  if (!leitorRfid.PICC_ReadCardSerial()) {
    return;
  }

  String uid = uidCartaoHex();
  Serial.print(TAG);
  Serial.print(" rfid uid=");
  Serial.println(uid);
  abrirPortaPorRfid(uid);

  leitorRfid.PICC_HaltA();
  leitorRfid.PCD_StopCrypto1();
  delay(800);
}
#endif

void setup() {
  Serial.begin(9600);
  delay(500);
  logLinha("boot Casa Automatica");

  if (strcmp(WIFI_SSID, "SUA_REDE_WIFI") == 0) {
    logLinha("AVISO: configure WIFI_SSID em config.h");
  }

  WiFi.onEvent(onWifiEvent);

  // Wi-Fi antes dos servos/RFID (menos interferencia e pico de corrente)
  conectarWifi();

  validarPinos();

#if ENABLE_SAIDA_AUX
  if (pinPermiteSaida(PIN_SAIDA_AUX)) {
    pinMode(PIN_SAIDA_AUX, OUTPUT);
    digitalWrite(PIN_SAIDA_AUX, HIGH);
  }
#endif

  if (pinPermiteSaida(PIN_CHUVA_POWER)) {
    pinMode(PIN_CHUVA_POWER, OUTPUT);
  }
  if (wroomPinPermiteEntrada(PIN_CHUVA_DO)) {
    pinMode(PIN_CHUVA_DO, INPUT);
  }

#if ENABLE_LEDS_LUZES
  if (pinPermiteSaida(PIN_LED_SALA)) pinMode(PIN_LED_SALA, OUTPUT);
  if (pinPermiteSaida(PIN_LED_QUARTO)) pinMode(PIN_LED_QUARTO, OUTPUT);
  if (pinPermiteSaida(PIN_LED_COZINHA)) pinMode(PIN_LED_COZINHA, OUTPUT);
  digitalWrite(PIN_LED_SALA, LOW);
  digitalWrite(PIN_LED_QUARTO, LOW);
  digitalWrite(PIN_LED_COZINHA, LOW);
#endif

  delay(300);
  configurarServos();

#if ENABLE_RFID
  configurarRfid();
#endif

  if (!wifiConectado()) {
    conectarWifi();
  }
  if (wifiConectado()) {
    configurarNtp();
    sincronizarEstadoInicial();
    sincronizarFilaEntradas();
  }

  logLinha("pronto");
  Serial.print(TAG);
  Serial.print(" API: ");
  Serial.println(API_BASE_URL);
}

void loop() {
  unsigned long agora = millis();

  if (agora - ultimaVerificacaoWifi >= INTERVALO_WIFI_CHECK_MS) {
    ultimaVerificacaoWifi = agora;
    bool estavaConectado = wifiConectado();
    if (!estavaConectado) {
      conectarWifi();
      if (wifiConectado()) {
        configurarNtp();
        sincronizarEstadoInicial();
        sincronizarFilaEntradas();
      }
    }
  }

  processarChuva();

  if (!portaoEmMovimento) {
    sincronizarPortao();
    sincronizarJanelas();
  }

  if (agora - ultimaSyncEntradas >= INTERVALO_SYNC_ENTRADAS_MS) {
    ultimaSyncEntradas = agora;
    sincronizarFilaEntradas();
  }

#if ENABLE_RFID
  processarRfid();
  processarAutoFecharPortaRfid();
#endif

#if ENABLE_LEDS_LUZES
  static unsigned long ultimaLuz = 0;
  if (agora - ultimaLuz >= 2000) {
    ultimaLuz = agora;
    sincronizarLuzes();
  }
#endif

  delay(50);
}
