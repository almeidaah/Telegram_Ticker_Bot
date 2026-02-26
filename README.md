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

## 🔧 Compilação e Execução Local

### Compilar o projeto

```bash
mvn clean package
```

### Executar os testes

```bash
mvn test
```

### Executar o bot

```bash
java -jar target/Telegram_Ticker_Bot-1.0-SNAPSHOT.jar
```

O bot ficará rodando em primeiro plano. Para parar, pressione `Ctrl+C`.

### Verificar se está funcionando

Abra o Telegram, encontre seu bot pelo username e envie `/start`. Se o bot responder, está funcionando.

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
            └── telegram/ticker/bot/
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

## ⚠️ Observações

- O arquivo `.env` contém dados sensíveis e **não deve ser commitado** (já está no `.gitignore`)
- O bot deve estar rodando continuamente para receber mensagens
- Os dados são buscados em tempo real a cada requisição

## 📝 Licença

Este projeto é de uso livre para fins educacionais e pessoais.
