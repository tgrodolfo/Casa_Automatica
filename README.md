# 🏠 Casa Automática — Controle de Ambientes

Sistema web para controle de iluminação de ambientes, desenvolvido com **Spring Boot** (back-end) e **Thymeleaf + HTML/CSS/JS** (front-end).

---

## 📋 Sobre o Projeto

A **Casa Automática** é uma aplicação de automação residencial que controla **luzes**, **janelas** e exibe o estado de **chuva** lido pelo ESP32. Por meio de uma interface web intuitiva, o usuário controla iluminação e aberturas; o sensor de chuva é atualizado apenas pelo firmware.

---

## 🚀 Funcionalidades

- Listar e controlar luzes por ambiente (ligar/desligar, toggle, ligar/apagar tudo)
- Listar e controlar janelas por ambiente (aberto/fechado, toggle, abrir/fechar tudo)
- Exibir se está chovendo (estado enviado pelo ESP32 via API)
- Histórico de momentos de chuva (início e fim com data/hora)
- Registro de entradas pela porta RFID (data/hora; fila offline no ESP)
- Tela `/logs` com calendário: clique no dia para ver chuva e entradas daquele dia
- Tela `/cartoes` para cadastrar o nome da pessoa por UID do cartão RFID
- Custom Skill Alexa para acender luzes, abrir/fechar janelas e portão
- Painéis de estatísticas em tempo real
- Notificações visuais (toast) para feedback de ações

---

## 🛠️ Tecnologias Utilizadas

### Back-end
- **Java 17+**
- **Spring Boot**
- **Spring Data JPA**
- **Hibernate**
- **Springdoc OpenAPI (Swagger)**
- **Banco de dados relacional** (ex.: H2, MySQL ou PostgreSQL)

### Front-end
- **Thymeleaf** (template engine)
- **HTML5 / CSS3 / JavaScript**
- Fontes: [Rajdhani](https://fonts.google.com/specimen/Rajdhani) e [JetBrains Mono](https://fonts.google.com/specimen/JetBrains+Mono)

---

## 📁 Estrutura do Projeto

```
src/
└── main/
    ├── java/
    │   └── luzes/coresystemstgr/
    │       └── quartos/
    │           ├── Quartos.java          # Entidade JPA
    │           ├── QuartosRepository.java
    │           ├── QuartosService.java
    │           └── QuartosController.java
    └── resources/
        ├── templates/
        │   └── index.html               # Interface principal (Thymeleaf)
        ├── static/
        │   └── style.css
        └── application.properties
```

---

## ⚙️ Como Executar

### Pré-requisitos

- Java 17 ou superior
- Maven 3.8+
- MySQL em execução

### Segurança (obrigatório)

A API **não inicia** sem credenciais configuradas. Copie o exemplo e defina valores fortes:

```bash
cp application.properties.example src/main/resources/application-local.properties
```

Edite `application-local.properties` (arquivo gitignored) ou use variáveis de ambiente:

| Variável | Descrição |
|----------|-----------|
| `CASA_AUTOMATICA_USER` | Usuário do painel web (padrão: `admin`) |
| `CASA_AUTOMATICA_PASSWORD` | Senha do painel (**mín. 12 caracteres**) |
| `CASA_AUTOMATICA_DEVICE_API_KEY` | Chave do ESP32 (**mín. 32 caracteres**) |
| `MYSQL_PASSWORD` | Senha do banco MySQL |
| `CASA_AUTOMATICA_RATE_LIMIT` | Requisições por IP por minuto (padrão: `120`) |

**Autenticação:**

- **Navegador:** login em `/login` (sessão + CSRF nos POSTs da interface).
- **ESP32:** header `X-API-Key` com a chave do dispositivo. Endpoints do device: `GET /api/quartos`, `GET /api/portao`, `GET /api/janelas`, `POST /api/chuva`, `POST /api/porta/entradas`.

**Exposição pública:** use HTTPS (Cloudflare Tunnel ou proxy reverso). Não abra porta no roteador sem TLS na frente.

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/casa-automatica.git
cd casa-automatica

# 2. Configure segredos (application-local.properties ou env vars)

# 3. Execute a aplicação
./mvnw spring-boot:run

# 4. Acesse no navegador e faça login
http://localhost:8000/login
```

---

## 🔌 Endpoints da API

Base: `http://localhost:8000`

### Luzes — `/api/quartos`

| Método | Rota                        | Descrição                              |
|--------|-----------------------------|----------------------------------------|
| GET    | `/api/quartos`              | Retorna todos os ambientes             |
| POST   | `/api/quartos`              | Cadastra um novo ambiente              |
| POST   | `/api/quartos/{id}/toggle`  | Alterna o estado da luz do ambiente    |
| POST   | `/api/quartos/all/on`       | Liga todas as luzes                    |
| POST   | `/api/quartos/all/off`      | Apaga todas as luzes                   |

Exemplo — `GET /api/quartos`:

```json
{
  "rooms": [
    {
      "id": 1,
      "room": "cozinha",
      "light": "on",
      "status": "iluminado"
    }
  ]
}
```

### Janelas — `/api/janelas`

| Método | Rota                          | Descrição                    |
|--------|-------------------------------|------------------------------|
| GET    | `/api/janelas`                | Lista todas as janelas       |
| POST   | `/api/janelas`                | Cadastra uma janela          |
| POST   | `/api/janelas/{id}/toggle`    | Alterna aberto/fechado       |
| POST   | `/api/janelas/all/open`       | Abre todas                   |
| POST   | `/api/janelas/all/closed`     | Fecha todas                  |

Exemplo — `GET /api/janelas`:

```json
{
  "windows": [
    {
      "id": 1,
      "window": "cozinha",
      "state": "closed",
      "status": "fechado"
    }
  ]
}
```

### Chuva (ESP32) — `/api/chuva`

| Método | Rota                    | Descrição                                      |
|--------|-------------------------|------------------------------------------------|
| GET    | `/api/chuva`            | Lê se está chovendo (interface web e ESP)      |
| GET    | `/api/chuva/momentos`   | Lista momentos de chuva (início/fim)           |
| POST   | `/api/chuva`            | **ESP32 envia** leitura do sensor de chuva     |

Corpo do `POST` (ESP32):

```json
{ "rain": "yes" }
```

Valores aceitos para `rain`: `yes` / `no` (também `on` / `off`).
Ao mudar de seco→chovendo ou chovendo→seco, o Java grava o momento com data/hora (`America/Sao_Paulo`).

Exemplo — `GET /api/chuva`:

```json
{
  "rain": "yes",
  "status": "chovendo"
}
```

Exemplo — `GET /api/chuva/momentos`:

```json
{
  "moments": [
    {
      "id": 1,
      "inicio": "2026-11-25T14:30:00",
      "fim": "2026-11-25T15:10:00",
      "emAndamento": false
    }
  ]
}
```

### Logs por dia — `/api/logs`

| Método | Rota                 | Descrição                                      |
|--------|----------------------|------------------------------------------------|
| GET    | `/api/logs/calendario?ano=&mes=` | Dias do mês com registros           |
| GET    | `/api/logs/dia?data=YYYY-MM-DD`  | Chuva + entradas daquele dia        |

Na interface: `/logs` (calendário) → clique no dia → `/logs?dia=YYYY-MM-DD`.

### Alexa — `/api/alexa`

| Método | Rota         | Descrição                                      |
|--------|--------------|------------------------------------------------|
| POST   | `/api/alexa` | Endpoint HTTPS da Custom Skill Alexa           |

Configure no [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask):

1. Crie uma Custom Skill em português (BR) com invocation name `casa automática`.
2. Cole o modelo de `alexa/interaction-model.pt-BR.json`.
3. Default endpoint HTTPS: `https://lar-doce-lar.coresystemstgr.com/api/alexa` (ou o host do tunnel).
4. Copie o Skill ID para `CASA_AUTOMATICA_ALEXA_SKILL_ID` / `casa-automatica.security.alexa-skill-id`.

Exemplos de fala:

- “Alexa, peça para a casa automática acender a luz da sala”
- “Alexa, peça para a casa automática abrir as janelas”
- “Alexa, peça para a casa automática abrir o portão”

### Cartões RFID — `/api/porta/cartoes`

| Método | Rota                          | Descrição                                      |
|--------|-------------------------------|------------------------------------------------|
| GET    | `/api/porta/cartoes`          | Lista UIDs conhecidos e nomes cadastrados      |
| PUT    | `/api/porta/cartoes/{cardUid}`| Salva/atualiza o nome da pessoa do cartão      |

Corpo do `PUT`:

```json
{ "nome": "Maria Silva" }
```

### Porta RFID — `/api/porta/entradas`

| Método | Rota                   | Descrição                                              |
|--------|------------------------|--------------------------------------------------------|
| GET    | `/api/porta/entradas`  | Lista entradas pela porta com data/hora                |
| POST   | `/api/porta/entradas`  | **ESP32 registra** entrada (fila offline se sem net)   |

Corpo do `POST` (ESP32):

```json
{ "cardUid": "A1B2C3D4", "ocorridoEm": "2026-11-25T14:30:00", "offline": false }
```

Se `ocorridoEm` for omitido, o servidor Java registra a hora atual. Sem internet, o ESP guarda na fila local e sincroniza depois (`offline: true`).

### Documentação interativa (Swagger)

```
http://localhost:8000/swagger-ui.html
```

---

## 🗄️ Modelo de Dados

### Luzes (`Quartos`)

| Campo    | Tipo    | Descrição                          |
|----------|---------|------------------------------------|
| `id`     | Long    | Identificador único                |
| `room`   | String  | Nome do ambiente (ex.: "cozinha")  |
| `light`  | String  | `"on"` ou `"off"`                  |
| `status` | String  | `"iluminado"` ou `"apagado"`       |

### Janelas (`Janela`)

| Campo    | Tipo    | Descrição                          |
|----------|---------|------------------------------------|
| `id`     | Long    | Identificador único                |
| `window` | String  | Nome do ambiente                   |
| `state`  | String  | `"open"` ou `"closed"`             |
| `status` | String  | `"aberto"` ou `"fechado"`          |

### Chuva (`Chuva` — registro único, sensor ESP)

| Campo    | Tipo    | Descrição                          |
|----------|---------|------------------------------------|
| `rain`   | String  | `"yes"` ou `"no"`                  |
| `status` | String  | `"chovendo"` ou `"sem chuva"`      |

### Momentos de chuva (`MomentoChuva`)

| Campo    | Tipo          | Descrição                                      |
|----------|---------------|------------------------------------------------|
| `id`     | Long          | Identificador                                  |
| `inicio` | LocalDateTime | Quando começou (hora Java)                     |
| `fim`    | LocalDateTime | Quando acabou (`null` se ainda chovendo)       |

### Entradas pela porta (`EntradaPorta`)

| Campo            | Tipo          | Descrição                                           |
|------------------|---------------|-----------------------------------------------------|
| `id`             | Long          | Identificador                                       |
| `cardUid`        | String        | UID do cartão RFID                                  |
| `ocorridoEm`     | LocalDateTime | Momento da entrada                                  |
| `sincronizadoEm` | LocalDateTime | Quando o servidor recebeu                           |
| `offline`        | boolean       | Veio da fila local do ESP                           |

---

## 🖥️ Interface

A interface conta com:

- **Header** com logo, badge de status do sistema e botões de ação global
- **Painel de estatísticas** com contadores de ambientes iluminados, apagados e total
- **Grade de cards** com cada ambiente cadastrado, exibindo ícone, nome, status e toggle interativo
- **Notificações toast** para feedback de ações bem-sucedidas ou erros

---

## 🤖 Firmware ESP32 (Arduino)

O projeto conta também com um cliente físico rodando em um **ESP32**, que:

1. **Lê** `GET /api/quartos` e aciona LEDs conforme `light`.
2. **Envia** `POST /api/chuva` com a leitura do sensor de chuva (a interface web só exibe esse valor).

### Bibliotecas necessárias

- `WiFi`, `HTTPClient` (nativas do ESP32)
- `ArduinoJson` (Benoit Blanchon)

### Mapeamento de pinos

| Função              | Pino GPIO |
|---------------------|-----------|
| LED Cozinha         | 2         |
| LED Quarto          | 4         |
| LED Sala            | 5         |
| Sensor de chuva     | 18        |

> No sensor de chuva, use `INPUT_PULLUP`: pino em **LOW** quando está chovendo (contato fechado), **HIGH** quando seco.

### Código (luzes + chuva)

```cpp
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* ssid = "SEU_SSID";
const char* password = "SUA_SENHA";
const char* serverBase = "http://SEU_IP:8000";

#define LED_COZINHA 2
#define LED_QUARTO  4
#define LED_SALA    5
#define PIN_CHUVA   18

void sincronizarLuzes() {
  HTTPClient http;
  http.begin(String(serverBase) + "/api/quartos");
  http.addHeader("X-API-Key", "SUA_CHAVE_DO_ESP32");  // mesma de CASA_AUTOMATICA_DEVICE_API_KEY
  int httpCode = http.GET();

  if (httpCode == HTTP_CODE_OK) {
    String payload = http.getString();
    DynamicJsonDocument doc(2048);
    if (!deserializeJson(doc, payload)) {
      for (JsonObject room : doc["rooms"].as<JsonArray>()) {
        String nome = room["room"].as<String>();
        nome.toLowerCase();
        bool ligado = room["light"].as<String>() == "on";
        int pin = -1;
        if (nome == "cozinha") pin = LED_COZINHA;
        else if (nome == "quarto") pin = LED_QUARTO;
        else if (nome == "sala") pin = LED_SALA;
        if (pin != -1) digitalWrite(pin, ligado ? HIGH : LOW);
      }
    }
  }
  http.end();
}

void enviarChuva() {
  bool chovendo = digitalRead(PIN_CHUVA) == LOW;
  HTTPClient http;
  http.begin(String(serverBase) + "/api/chuva");
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-Key", "SUA_CHAVE_DO_ESP32");  // mesma de CASA_AUTOMATICA_DEVICE_API_KEY
  String body = chovendo ? "{\"rain\":\"yes\"}" : "{\"rain\":\"no\"}";
  http.POST(body);
  http.end();
}

void setup() {
  Serial.begin(9600);
  pinMode(LED_COZINHA, OUTPUT);
  pinMode(LED_QUARTO, OUTPUT);
  pinMode(LED_SALA, OUTPUT);
  pinMode(PIN_CHUVA, INPUT_PULLUP);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) delay(500);
}

void loop() {
  sincronizarLuzes();
  enviarChuva();
  delay(300);
}
```

### Como funciona

1. O ESP32 conecta-se ao Wi-Fi e, a cada **300 ms**, busca as luzes e atualiza os LEDs.
2. Na mesma cadência, lê o pino do sensor de chuva e faz **`POST /api/chuva`** com `"rain": "yes"` ou `"no"`.
3. A interface web consulta **`GET /api/chuva`** e mostra chovendo / sem chuva (somente leitura).

> Substitua `SEU_SSID`, `SUA_SENHA` e `SEU_IP`. ESP32 e servidor devem estar na **mesma rede local**.

---

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

---

> Desenvolvido com ☕ Java e ⚡ ESP32 por **Luma K. de Siqueira** e **Tales G. Rodolfo**.