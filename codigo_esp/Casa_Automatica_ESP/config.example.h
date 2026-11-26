#pragma once

// Copie para config.h — pinagem ESP-WROOM-32 (ver comentario em config.h)

#define WIFI_SSID "SUA_REDE_WIFI"
#define WIFI_PASSWORD "SUA_SENHA_WIFI"
// Rede 2.4 GHz, WPA2-PSK (ESP32 nao suporta WPA3 sozinho)

#define API_BASE_URL "https://lar-doce-lar.coresystemstgr.com"
#define API_KEY "local-device-api-key-32-characters-ok"

#define INTERVALO_CHUVA_MS 2000
#define INTERVALO_PORTAO_MS 1000
#define INTERVALO_JANELAS_MS 1000
#define INTERVALO_WIFI_CHECK_MS 15000
#define WIFI_CONNECT_TIMEOUT_MS 30000
#define HTTP_TIMEOUT_MS 15000
#define FILA_ENTRADAS_MAX 32
#define INTERVALO_SYNC_ENTRADAS_MS 5000
#define NTP_GMT_OFFSET_SEC (-3 * 3600)
#define NTP_DAYLIGHT_OFFSET_SEC 0

#define PIN_SERVO_PORTAO 14
#define ANGULO_PORTAO_ABERTO 140
#define ANGULO_PORTAO_FECHADO 80
#define PORTAO_GRAUS_POR_MS 2

// Janela da sala: 2 servos (as duas partes) que abrem/fecham juntos.
#define JANELA_SALA_NOME "sala"
#define PIN_SERVO_JANELA_1 32
#define ANGULO_JANELA_1_ABERTO 150
#define ANGULO_JANELA_1_FECHADO 20
#define PIN_SERVO_JANELA_2 33
#define ANGULO_JANELA_2_ABERTO 150
#define ANGULO_JANELA_2_FECHADO 40

// Garagem: 2 servos (as duas partes) que abrem/fecham juntos.
#define JANELA_GARAGEM_NOME "garagem"
#define PIN_SERVO_GARAGEM_1 26
#define ANGULO_GARAGEM_1_ABERTO 180
#define ANGULO_GARAGEM_1_FECHADO 15
#define PIN_SERVO_GARAGEM_2 27
#define ANGULO_GARAGEM_2_ABERTO 10
#define ANGULO_GARAGEM_2_FECHADO 150

#define PIN_SERVO_PORTA_RFID 12
#define ANGULO_PORTA_RFID_ABERTO 180
#define ANGULO_PORTA_RFID_FECHADO 0
#define PORTA_RFID_AUTO_FECHAR_MS 5000  // 0 desliga o auto-fechar

#define ENABLE_SAIDA_AUX false

#define PIN_CHUVA_POWER 13
#define PIN_CHUVA_DO 35

#define ENABLE_RFID true
#define PIN_RFID_SS 5
#define PIN_RFID_RST 22
#define PIN_RFID_SCK 18
#define PIN_RFID_MISO 19
#define PIN_RFID_MOSI 23

#define ENABLE_LEDS_LUZES true
#define PIN_LED_SALA 25
#define PIN_LED_QUARTO 15
#define PIN_LED_COZINHA 2
