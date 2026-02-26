# 📘 Instruções — Telegram Ticker Bot

## Resumo do Projeto

O **Telegram Ticker Bot** é um **bot do Telegram** que funciona como um agregador de informações financeiras. Ele cobre **4 tipos de ativos**:

## 📊 Ativos Suportados

| Tipo | Padrão do Ticker | Exemplo | Fonte de Dados |
|------|-------------------|---------|----------------|
| **Ações Ibovespa** | `XXXX3` ou `XXXX4` | `PETR3, VALE3` | StatusInvest |
| **Ações EUA** | Prefixo `$` | `$AAPL, $MSFT` | Yahoo Finance |
| **Fundos Imobiliários** | `XXXX11` | `HGLG11, MXRF11` | Funds Explorer |
| **Criptomoedas** | Símbolo | `BTC, ETH, SOL` | CoinGecko |

## 🔄 Como funciona

1. **Usuário envia tickers** no chat do Telegram (pode misturar tipos)
2. O bot **classifica** automaticamente cada ticker (ação BR, ação US, fundos ou crypto)
3. **Busca indicadores fundamentalistas** via scraping/APIs (P/L, DY, P/VP, ROE, etc.)
4. **Busca até 3 notícias recentes** (máximo 1 mês) no Google News RSS
5. **Retorna tudo formatado** em tabelas + links das notícias

## 📋 Indicadores por ativo

### Ações Ibovespa
- P/L (Preço/Lucro)
- Dividend Yield
- P/VP (Preço/Valor Patrimonial)
- ROE (Retorno sobre Patrimônio)
- Dívida Líquida/EBITDA
- Margem EBITDA
- LPA (Lucro por Ação)
- Crescimento de Receita 5 anos

### Ações EUA
- P/E (Price/Earnings)
- P/S (Price/Sales)
- EV/EBITDA
- P/BV (Price/Book Value)
- PEG (P/E ajustado por crescimento)
- FCF Yield (Free Cash Flow Yield)
- ROE (Return on Equity)
- Margem EBITDA
- Dívida/EBITDA
- Current Ratio (Liquidez Corrente)
- Dividend Yield
- Payout Ratio

### Fundos Imobiliários
- Dividend Yield
- P/VP (Preço/Valor Patrimonial)
- Número de Cotistas
- Número de Imóveis

### Criptomoedas
- Preço atual (USD)
- Market Cap
- Ranking entre criptomoedas

## 🚀 Como rodar o projeto

### Pré-requisitos
- Java 17+
- Maven 3.6+
- Arquivo `.env` com as variáveis:
  ```
  TELEGRAM_BOT_TOKEN=seu_token_do_telegram
  TELEGRAM_BOT_USERNAME=seu_username_do_bot
  ```

### Compilar e executar
```bash
mvn clean package
java -jar target/Telegram_Ticker_Bot-1.0-SNAPSHOT.jar
```

## 💬 Como usar no Telegram

1. Abra o Telegram e procure por seu bot
2. Envie `/start` para ver as instruções
3. Envie os tickers separados por vírgula:

### Exemplos de uso
```
PETR3, VALE3, ITUB4          → Ações Ibovespa
$AAPL, $MSFT, $GOOGL         → Ações EUA
HGLG11, MXRF11               → Fundos Imobiliários
BTC, ETH, SOL                → Criptomoedas
PETR3, $AAPL, HGLG11, BTC    → Misto (todos os tipos)
```

## 🏗️ Stack Técnica

- **Java** + **Maven** (shaded JAR)
- **TelegramBots API** (long polling)
- **Jsoup** — scraping do Funds Explorer, StatusInvest e Yahoo Finance
- **CoinGecko API** — dados de criptomoedas
- **Google News RSS** — notícias (até 3 por ativo, máximo 1 mês)

## 🔍 Verificar se o bot está rodando

```bash
# Verificar processo
pgrep -f "Telegram_Ticker_Bot" && echo "Rodando" || echo "Parado"

# Testar conexão com o Telegram
curl -s "https://api.telegram.org/bot<SEU_TOKEN>/getMe" | python3 -m json.tool
```

## 👨‍💻 Criador

**Fernando Almeida**
- 🐙 GitHub: [https://github.com/almeidaah](https://github.com/almeidaah)
- 𝕏 X/Twitter: [https://x.com/faflpx](https://x.com/faflpx)
