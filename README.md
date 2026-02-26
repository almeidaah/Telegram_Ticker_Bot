# Telegram Ticker Bot 📰

Bot do Telegram para buscar indicadores financeiros e notícias de ativos brasileiros, americanos e criptomoedas usando o Google News RSS como fonte.

## 🚀 Funcionalidades

- Suporta **Ações Brasileiras** (Ibovespa) — P/L, DY, P/VP, ROE, Dív/EBITDA, LPA
- Suporta **Ações Americanas** (US Stocks) — P/E, P/S, EV/EBITDA, PEG, ROE, FCF Yield
- Suporta **Fundos Imobiliários** — Dividend Yield, P/VP, Cotistas, Imóveis
- Suporta **Criptomoedas** — Preço, Market Cap, Ranking
- Busca notícias recentes de cada ativo no Google News
- Retorna até 3 notícias por ativo (último mês)
- Formatação amigável com tabelas e emojis

## 📋 Pré-requisitos

- Java 17 ou superior
- Maven 3.6+
- Token de bot do Telegram (obtido via [@BotFather](https://t.me/botfather))

## ⚙️ Configuração

1. Clone o repositório
2. Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```env
TELEGRAM_BOT_TOKEN=seu_token_aqui
TELEGRAM_BOT_USERNAME=nome_do_seu_bot
```

## 🔧 Compilação e Execução

### Compilar o projeto

```bash
mvn clean package
```

### Executar o bot

```bash
java -jar target/Telegram_Ticker_Bot-1.0-SNAPSHOT.jar
```

Ou usando Maven:

```bash
mvn exec:java -Dexec.mainClass="com.scibite.pipelines.Main"
```

## 💬 Como usar o bot

1. Abra o Telegram e encontre seu bot pelo username
2. Envie uma mensagem com os códigos dos ativos separados por vírgulas
3. Use `$` antes de tickers americanos

**Exemplos:**
```
PETR3, VALE3, ITUB4
```
```
$AAPL, $MSFT, $GOOGL
```
```
HGLG11, KNRI11, MXRF11
```
```
BTC, ETH, SOL
```
```
PETR3, $AAPL, HGLG11, BTC
```

**Resposta:**
```
📊 Indicadores e Notícias de Ações

📈 PETR3
┌────────────────┬────────────┐
│ P/L            │       8.45 │
│ Div. Yield     │      12.3% │
│ P/VP           │       1.12 │
...
└────────────────┴────────────┘

   👉 Petrobras anuncia novo plano de investimentos
   🔗 https://news.google.com/...

   👉 Preço do petróleo influencia ações da PETR3
   🔗 https://news.google.com/...
```

## 📁 Estrutura do Projeto

```
Telegram_Ticker_Bot/
├── pom.xml                          # Configuração Maven
├── .env                             # Variáveis de ambiente (não versionar!)
├── .gitignore                       # Arquivos ignorados pelo Git
├── README.md                        # Esta documentação
└── src/
    └── main/
        └── java/
            └── com/scibite/pipelines/
                ├── Main.java               # Classe principal
                ├── TelegramTickerBot.java  # Bot do Telegram
                ├── NewsFetcher.java        # Busca de notícias
                ├── NewsItem.java           # Modelo de notícia
                ├── FIIData.java            # Dados de fundos imobiliários
                ├── FIIDataFetcher.java     # Busca dados de fundos
                ├── StockData.java          # Dados de ações BR
                ├── StockDataFetcher.java   # Busca dados de ações BR
                ├── USStockData.java        # Dados de ações US
                ├── USStockDataFetcher.java # Busca dados de ações US
                ├── CryptoData.java         # Dados de criptomoedas
                └── CryptoDataFetcher.java  # Busca dados de crypto
```

## 🔍 Detalhes Técnicos

### Fonte de Notícias
O bot utiliza o **Google News RSS** para buscar notícias. A busca é feita com queries específicas para cada tipo de ativo.

URL do feed: `https://news.google.com/rss/search?q={query}&hl=pt-BR&gl=BR&ceid=BR:pt-419`

### Bibliotecas Utilizadas
- **TelegramBots 6.9.7.1** - API para bots do Telegram
- **Jsoup 1.17.2** - Parser de HTML/XML
- **dotenv-java 3.0.0** - Carregamento de variáveis de ambiente
- **SLF4J 2.0.9** - Logging

## 🌐 Deploy Gratuito 24/7

Este bot usa **long polling** (não precisa de porta HTTP aberta), então funciona como um **worker/background process**. Abaixo estão as melhores opções gratuitas para mantê-lo rodando 24/7.

### Comparativo de Plataformas Gratuitas

| Plataforma | Sempre Ligado? | Recursos Free | Dificuldade |
|---|---|---|---|
| **Oracle Cloud** | ✅ Sim | VM ARM 4 OCPUs + 24GB RAM | Média |
| **Fly.io** | ✅ Sim | 3 VMs shared, 256MB cada | Fácil |
| **Railway** | ✅ Sim | $5 crédito/mês (~720h) | Muito Fácil |
| **Render** | ❌ Dorme | Background worker dorme | Fácil |

---

### Opção 1: Fly.io (Recomendado — Fácil + Sempre Ligado)

1. **Crie uma conta** em [fly.io](https://fly.io) (requer cartão, mas não cobra)

2. **Instale o CLI:**
   ```bash
   # macOS
   brew install flyctl
   # Linux
   curl -L https://fly.io/install.sh | sh
   ```

3. **Faça login:**
   ```bash
   fly auth login
   ```

4. **Crie o app (primeira vez):**
   ```bash
   fly launch --no-deploy
   ```

5. **Configure os secrets (variáveis de ambiente):**
   ```bash
   fly secrets set TELEGRAM_BOT_TOKEN=seu_token_aqui
   fly secrets set TELEGRAM_BOT_USERNAME=nome_do_bot
   ```

6. **Deploy:**
   ```bash
   fly deploy
   ```

7. **Verificar logs:**
   ```bash
   fly logs
   ```

> O arquivo `fly.toml` já está configurado na raiz do projeto com região `gru` (São Paulo).

---

### Opção 2: Railway (Mais Fácil — $5/mês grátis)

1. **Crie uma conta** em [railway.app](https://railway.app) com GitHub

2. **Novo projeto** → "Deploy from GitHub repo" → selecione `Telegram_Ticker_Bot`

3. **Configure as variáveis de ambiente** no painel do Railway:
   - `TELEGRAM_BOT_TOKEN` = seu token
   - `TELEGRAM_BOT_USERNAME` = nome do bot

4. **Railway detecta automaticamente** o `Procfile` e `system.properties` e faz o deploy

> O plano gratuito dá $5/mês de crédito, suficiente para rodar o bot 24/7 em uma instância pequena.

---

### Opção 3: Oracle Cloud Free Tier (Mais Recursos — Sempre Grátis)

Melhor opção se você quer um servidor de verdade, com recursos generosos e sem limite de tempo.

1. **Crie uma conta** em [cloud.oracle.com](https://cloud.oracle.com) (requer cartão, não cobra)

2. **Crie uma instância VM** (Compute → Create Instance):
   - Shape: `VM.Standard.A1.Flex` (ARM) — até 4 OCPUs, 24GB RAM grátis
   - OS: Oracle Linux 8 ou Ubuntu
   - Salve a chave SSH

3. **Conecte via SSH:**
   ```bash
   ssh -i sua_chave.pem ubuntu@IP_DA_INSTANCIA
   ```

4. **Instale Java 17:**
   ```bash
   sudo apt update && sudo apt install -y openjdk-17-jre-headless
   ```

5. **Transfira e execute o JAR:**
   ```bash
   # No seu PC, faça build e envie:
   mvn clean package -DskipTests
   scp -i sua_chave.pem target/Telegram_Ticker_Bot-1.0-SNAPSHOT.jar ubuntu@IP:/home/ubuntu/

   # No servidor, crie um serviço systemd:
   sudo tee /etc/systemd/system/ticker-bot.service << 'EOF'
   [Unit]
   Description=Telegram Ticker Bot
   After=network.target

   [Service]
   Type=simple
   User=ubuntu
   WorkingDirectory=/home/ubuntu
   Environment=TELEGRAM_BOT_TOKEN=seu_token_aqui
   Environment=TELEGRAM_BOT_USERNAME=nome_do_bot
   ExecStart=/usr/bin/java -jar /home/ubuntu/Telegram_Ticker_Bot-1.0-SNAPSHOT.jar
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   EOF

   sudo systemctl enable ticker-bot
   sudo systemctl start ticker-bot
   ```

6. **Verificar status:**
   ```bash
   sudo systemctl status ticker-bot
   sudo journalctl -u ticker-bot -f
   ```

---

### Deploy com Docker (qualquer plataforma)

O projeto inclui um `Dockerfile` multi-stage otimizado:

```bash
# Build e executar localmente
docker build -t ticker-bot .
docker run -d --name ticker-bot \
  -e TELEGRAM_BOT_TOKEN=seu_token \
  -e TELEGRAM_BOT_USERNAME=nome_do_bot \
  --restart unless-stopped \
  ticker-bot
```

---

### CI/CD com GitHub Actions

O workflow em `.github/workflows/build-deploy.yml` faz build e teste automaticamente a cada push. Para habilitar deploy automático no Fly.io:

1. Gere um token: `fly tokens create deploy -x 999999h`
2. Adicione como secret no GitHub: `Settings → Secrets → FLY_API_TOKEN`
3. Descomente o job `deploy-fly` no arquivo do workflow

---

## ⚠️ Observações

- O arquivo `.env` contém informações sensíveis e não deve ser versionado
- O bot deve estar rodando continuamente para receber mensagens
- Os dados são buscados em tempo real a cada requisição
- Em plataformas cloud, use **variáveis de ambiente** do painel (não `.env`)

## 📝 Licença

Este projeto é de uso livre para fins educacionais e pessoais.
