package telegram.ticker.bot;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Bot do Telegram para buscar indicadores financeiros e notícias de ativos.
 *
 * Funcionalidades:
 * - Recebe lista de tickers de ações BR, ações US, FIIs e/ou símbolos de criptomoedas
 * - Busca indicadores financeiros e notícias recentes
 * - Para Ações BR: P/L, DY, P/VP, ROE, Dív.Líq/EBITDA, Margem EBITDA, LPA, Cresc.Rec 5a + notícias
 * - Para Ações US: P/E, P/S, EV/EBITDA, P/BV, PEG, FCF Yield, ROE, Margem EBITDA, etc + notícias (EN)
 * - Para FIIs: Dividend Yield, P/VP, Cotistas, Imóveis + notícias
 * - Para Crypto: Preço, Market Cap, Ranking + notícias
 * - Retorna até 3 notícias por ativo (último mês)
 * - Responde com mensagem de instrução para comandos inválidos
 */
public class TelegramTickerBot extends TelegramLongPollingBot {

    // Token do bot fornecido pelo BotFather
    private final String botToken;

    // Nome de usuário do bot no Telegram
    private final String botUsername;

    // Classe utilitária para buscar notícias
    private final NewsFetcher newsFetcher;

    // Classe utilitária para buscar indicadores financeiros de FIIs
    private final FIIDataFetcher fiiDataFetcher;

    // Classe utilitária para buscar indicadores de criptomoedas
    private final CryptoDataFetcher cryptoDataFetcher;

    // Classe utilitária para buscar indicadores de ações
    private final StockDataFetcher stockDataFetcher;

    // Classe utilitária para buscar indicadores de ações americanas
    private final USStockDataFetcher usStockDataFetcher;

    // Classe utilitária para encurtar URLs
    private final UrlShortener urlShortener;

    // Padrão para validar tickers de Fundos Imobiliários (ex: HGLG11, MXRF11)
    // Aceita 4 letras maiúsculas/minúsculas seguidas de 2 dígitos
    private static final Pattern FII_TICKER_PATTERN = Pattern.compile("^[A-Za-z]{4}\\d{2}$");

    // Padrão para validar tickers de ações (ex: PETR3, VALE3, ITUB4)
    // Aceita 4 letras maiúsculas/minúsculas seguidas de 1 dígito
    private static final Pattern STOCK_TICKER_PATTERN = Pattern.compile("^[A-Za-z]{4}\\d$");

    // Padrão para validar símbolos de criptomoedas (ex: BTC, ETH, SOL)
    // Aceita 2-10 letras maiúsculas/minúsculas
    private static final Pattern CRYPTO_SYMBOL_PATTERN = Pattern.compile("^[A-Za-z]{2,10}$");

    // Padrão para validar tickers de ações americanas (com prefixo $)
    // Aceita $AAPL, $MSFT, $GOOGL, $TSLA ($ + 1-5 letras)
    private static final Pattern US_STOCK_PATTERN = Pattern.compile("^\\$[A-Za-z]{1,5}$");

    // Máximo de notícias por ticker
    private static final int MAX_NEWS_PER_TICKER = 3;

    // Máximo de ativos por requisição
    private static final int MAX_ASSETS_PER_REQUEST = 3;

    /**
     * Construtor do bot.
     *
     * @param botToken Token do bot do Telegram
     * @param botUsername Nome de usuário do bot
     */
    public TelegramTickerBot(String botToken, String botUsername) {
        this(botToken, botUsername, false);
    }

    /**
     * Construtor do bot com opção de desabilitar verificação SSL.
     *
     * @param botToken Token do bot do Telegram
     * @param botUsername Nome de usuário do bot
     * @param disableSslVerification Se true, desabilita verificação SSL (apenas para NewsFetcher)
     */
    public TelegramTickerBot(String botToken, String botUsername, boolean disableSslVerification) {
        super(new DefaultBotOptions());
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.newsFetcher = new NewsFetcher(disableSslVerification);
        this.fiiDataFetcher = new FIIDataFetcher(disableSslVerification);
        this.cryptoDataFetcher = new CryptoDataFetcher(disableSslVerification);
        this.stockDataFetcher = new StockDataFetcher(disableSslVerification);
        this.usStockDataFetcher = new USStockDataFetcher(disableSslVerification);
        this.urlShortener = new UrlShortener();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Método chamado quando o bot recebe uma atualização (mensagem).
     * Processa a mensagem do usuário e responde com as notícias ou instruções.
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Verifica se a atualização contém uma mensagem de texto
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();

        System.out.println("📩 Mensagem recebida: " + messageText);

        // Verifica se é o comando /start ou /help
        if (messageText.startsWith("/start") || messageText.startsWith("/help")) {
            sendWelcomeMessage(chatId);
            return;
        }

        // Classifica os símbolos em FIIs, ações BR, ações US e criptomoedas
        List<String> fiiTickers = new ArrayList<>();
        List<String> stockTickers = new ArrayList<>();
        List<String> usStockTickers = new ArrayList<>();
        List<String> cryptoSymbols = new ArrayList<>();
        parseAndClassifySymbols(messageText, fiiTickers, stockTickers, usStockTickers, cryptoSymbols);

        if (fiiTickers.isEmpty() && stockTickers.isEmpty() && usStockTickers.isEmpty() && cryptoSymbols.isEmpty()) {
            sendInstructionMessage(chatId);
            return;
        }

        // Verifica se o total de ativos excede o limite permitido
        int totalAssets = fiiTickers.size() + stockTickers.size() + usStockTickers.size() + cryptoSymbols.size();
        if (totalAssets > MAX_ASSETS_PER_REQUEST) {
            sendTooManyAssetsMessage(chatId, totalAssets);
            return;
        }

        // Processa FIIs
        if (!fiiTickers.isEmpty()) {
            processTickersAndSendNews(chatId, fiiTickers);
        }

        // Processa ações BR
        if (!stockTickers.isEmpty()) {
            processStocksAndSendNews(chatId, stockTickers);
        }

        // Processa ações US
        if (!usStockTickers.isEmpty()) {
            processUSStocksAndSendNews(chatId, usStockTickers);
        }

        // Processa criptomoedas
        if (!cryptoSymbols.isEmpty()) {
            processCryptoAndSendNews(chatId, cryptoSymbols);
        }
    }

    /**
     * Classifica os símbolos da mensagem em fundos imobiliários, ações BR, ações US e criptomoedas.
     *
     * Regras de classificação:
     * - Fundos Imobiliários: 4 letras + 2 dígitos (ex: HGLG11, MXRF11)
     * - Ação BR: 4 letras + 1 dígito (ex: PETR3, VALE3, ITUB4)
     * - Ação US: prefixo $ + 1-5 letras (ex: $AAPL, $MSFT, $GOOGL)
     * - Crypto: 2-10 letras sem dígitos e reconhecida no CoinGecko (ex: BTC, ETH)
     *
     * @param message         Mensagem enviada pelo usuário
     * @param fiiTickers      Lista para receber tickers de fundos imobiliários válidos
     * @param stockTickers    Lista para receber tickers de ações BR válidos
     * @param usStockTickers  Lista para receber tickers de ações US válidos
     * @param cryptoSymbols   Lista para receber símbolos de criptomoedas válidos
     */
    private void parseAndClassifySymbols(String message, List<String> fiiTickers,
                                          List<String> stockTickers, List<String> usStockTickers,
                                          List<String> cryptoSymbols) {
        Arrays.stream(message.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .forEach(symbol -> {
                    String upper = symbol.toUpperCase();
                    if (US_STOCK_PATTERN.matcher(symbol).matches()) {
                        // Remove o $ e adiciona o ticker puro
                        usStockTickers.add(upper.substring(1));
                    } else if (FII_TICKER_PATTERN.matcher(upper).matches()) {
                        fiiTickers.add(upper);
                    } else if (STOCK_TICKER_PATTERN.matcher(upper).matches()) {
                        stockTickers.add(upper);
                    } else if (CRYPTO_SYMBOL_PATTERN.matcher(upper).matches()
                               && CryptoDataFetcher.isKnownCrypto(upper)) {
                        cryptoSymbols.add(upper);
                    }
                });
    }

    /**
     * Processa a lista de tickers, busca notícias e envia a resposta formatada.
     *
     * @param chatId ID do chat para enviar a resposta
     * @param tickers Lista de tickers de Fundos Imobiliários
     */
    private void processTickersAndSendNews(long chatId, List<String> tickers) {
        StringBuilder response = new StringBuilder();
        response.append("📊 *Indicadores e Notícias de Fundos Imobiliários*\n\n");

        for (String ticker : tickers) {
            System.out.println("🔍 Buscando dados para: " + ticker);

            // --- Tabela de indicadores ---
            try {
                FIIData data = fiiDataFetcher.fetchData(ticker);
                response.append(formatIndicatorsTable(ticker, data));
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao buscar indicadores de " + ticker + ": " + e.getMessage());
                // Mostra tabela com N/D se falhar
                response.append(formatIndicatorsTable(ticker, new FIIData(ticker, "N/D", "N/D", "N/D", "N/D")));
            }

            // --- Notícias ---
            try {
                List<NewsItem> newsItems = newsFetcher.fetchNews(ticker, MAX_NEWS_PER_TICKER);

                if (newsItems.isEmpty()) {
                    response.append("   _Notícia recente não encontrada._\n\n");
                } else {
                    for (NewsItem news : newsItems) {
                        response.append("   👉 ").append(escapeMarkdown(news.getTitle())).append("\n");
                        response.append("   🔗 ").append(urlShortener.shorten(news.getLink())).append("\n");
                    }
                    response.append("\n");
                }

            } catch (Exception e) {
                System.err.println("❌ Erro ao buscar notícias para " + ticker + ": " + e.getMessage());
                response.append("   ⚠️ _Erro ao buscar notícias._\n\n");
            }
        }

        sendMessage(chatId, response.toString());
    }

    /**
     * Processa a lista de ações, busca indicadores e notícias, e envia a resposta.
     *
     * @param chatId       ID do chat para enviar a resposta
     * @param stockTickers Lista de tickers de ações
     */
    private void processStocksAndSendNews(long chatId, List<String> stockTickers) {
        StringBuilder response = new StringBuilder();
        response.append("📊 *Indicadores e Notícias de Ações*\n\n");

        for (String ticker : stockTickers) {
            System.out.println("🔍 Buscando dados da ação: " + ticker);

            // --- Tabela de indicadores ---
            try {
                StockData data = stockDataFetcher.fetchData(ticker);
                response.append(formatStockTable(ticker, data));
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao buscar indicadores de " + ticker + ": " + e.getMessage());
                response.append(formatStockTable(ticker,
                        new StockData(ticker, "N/D", "N/D", "N/D", "N/D", "N/D", "N/D", "N/D", "N/D")));
            }

            // --- Notícias ---
            try {
                List<NewsItem> newsItems = newsFetcher.fetchStockNews(ticker, MAX_NEWS_PER_TICKER);

                if (newsItems.isEmpty()) {
                    response.append("   _Notícia recente não encontrada._\n\n");
                } else {
                    for (NewsItem news : newsItems) {
                        response.append("   👉 ").append(escapeMarkdown(news.getTitle())).append("\n");
                        response.append("   🔗 ").append(urlShortener.shorten(news.getLink())).append("\n");
                    }
                    response.append("\n");
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao buscar notícias para " + ticker + ": " + e.getMessage());
                response.append("   ⚠️ _Erro ao buscar notícias._\n\n");
            }
        }

        sendMessage(chatId, response.toString());
    }

    /**
     * Processa a lista de ações americanas, busca indicadores e notícias (em inglês).
     *
     * @param chatId         ID do chat para enviar a resposta
     * @param usStockTickers Lista de tickers de ações americanas (sem $)
     */
    private void processUSStocksAndSendNews(long chatId, List<String> usStockTickers) {
        StringBuilder response = new StringBuilder();
        response.append("🇺🇸 *US Stocks — Indicators & News*\n\n");

        for (String ticker : usStockTickers) {
            System.out.println("🔍 Buscando dados US stock: " + ticker);

            // --- Tabela de indicadores ---
            try {
                USStockData data = usStockDataFetcher.fetchData(ticker);
                response.append(formatUSStockTable(ticker, data));
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao buscar indicadores de $" + ticker + ": " + e.getMessage());
                response.append(formatUSStockTable(ticker,
                        new USStockData(ticker, ticker,
                                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A",
                                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A")));
            }

            // --- Notícias (em inglês) ---
            try {
                List<NewsItem> newsItems = newsFetcher.fetchUSStockNews(ticker, MAX_NEWS_PER_TICKER);

                if (newsItems.isEmpty()) {
                    response.append("   _No recent news found._\n\n");
                } else {
                    for (NewsItem news : newsItems) {
                        response.append("   👉 ").append(escapeMarkdown(news.getTitle())).append("\n");
                        response.append("   🔗 ").append(urlShortener.shorten(news.getLink())).append("\n");
                    }
                    response.append("\n");
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao buscar notícias para $" + ticker + ": " + e.getMessage());
                response.append("   ⚠️ _Error fetching news._\n\n");
            }
        }

        sendMessage(chatId, response.toString());
    }

    /**
     * Processa a lista de criptomoedas, busca dados e notícias, e envia a resposta.
     *
     * @param chatId        ID do chat para enviar a resposta
     * @param cryptoSymbols Lista de símbolos de criptomoedas
     */
    private void processCryptoAndSendNews(long chatId, List<String> cryptoSymbols) {
        StringBuilder response = new StringBuilder();
        response.append("🪙 *Indicadores e Notícias de Criptomoedas*\n\n");

        for (String symbol : cryptoSymbols) {
            System.out.println("🔍 Buscando dados crypto para: " + symbol);

            // --- Tabela de indicadores crypto ---
            CryptoData cryptoData = null;
            try {
                cryptoData = cryptoDataFetcher.fetchData(symbol);
                response.append(formatCryptoTable(symbol, cryptoData));
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao buscar dados de " + symbol + ": " + e.getMessage());
                response.append(formatCryptoTable(symbol, new CryptoData(symbol, symbol, "N/D", "N/D", "N/D")));
            }

            // --- Notícias crypto ---
            try {
                String cryptoName = (cryptoData != null) ? cryptoData.getName() : symbol;
                List<NewsItem> newsItems = newsFetcher.fetchCryptoNews(symbol, cryptoName, MAX_NEWS_PER_TICKER);

                if (newsItems.isEmpty()) {
                    response.append("   _Notícia recente não encontrada._\n\n");
                } else {
                    for (NewsItem news : newsItems) {
                        response.append("   👉 ").append(escapeMarkdown(news.getTitle())).append("\n");
                        response.append("   🔗 ").append(urlShortener.shorten(news.getLink())).append("\n");
                    }
                    response.append("\n");
                }
            } catch (Exception e) {
                System.err.println("❌ Erro ao buscar notícias para " + symbol + ": " + e.getMessage());
                response.append("   ⚠️ _Erro ao buscar notícias._\n\n");
            }
        }

        sendMessage(chatId, response.toString());
    }

    /**
     * Formata a tabela de indicadores financeiros para um Fundo Imobiliário.
     * Usa caracteres Unicode box-drawing para simular uma tabela no Telegram.
     *
     * @param ticker Código do fundo
     * @param data   Indicadores financeiros do fundo
     * @return String formatada com a tabela
     */
    private String formatIndicatorsTable(String ticker, FIIData data) {
        StringBuilder table = new StringBuilder();
        table.append("📈 *").append(ticker).append("*\n");
        table.append("```\n");
        table.append("┌──────────────┬────────────┐\n");
        table.append(String.format("│ Div. Yield   │ %10s │%n", data.getDividendYield()));
        table.append(String.format("│ P/VP         │ %10s │%n", data.getPVp()));
        table.append(String.format("│ Cotistas     │ %10s │%n", data.getCotistas()));
        table.append(String.format("│ Imóveis      │ %10s │%n", data.getImoveis()));
        table.append("└──────────────┴────────────┘\n");
        table.append("```\n");
        return table.toString();
    }

    /**
     * Formata a tabela de indicadores financeiros para uma ação brasileira.
     *
     * @param ticker Código da ação
     * @param data   Indicadores financeiros da ação
     * @return String formatada com a tabela
     */
    private String formatStockTable(String ticker, StockData data) {
        StringBuilder table = new StringBuilder();
        table.append("📈 *").append(ticker).append("*\n");
        table.append("```\n");
        table.append("┌────────────────┬────────────┐\n");
        table.append(String.format("│ P/L            │ %10s │%n", data.getPl()));
        table.append(String.format("│ Div. Yield     │ %10s │%n", data.getDividendYield()));
        table.append(String.format("│ P/VP           │ %10s │%n", data.getPVp()));
        table.append(String.format("│ ROE            │ %10s │%n", data.getRoe()));
        table.append(String.format("│ Dív.Líq/EBITDA │ %10s │%n", data.getDividaLiquidaEbitda()));
        table.append(String.format("│ Marg. EBITDA   │ %10s │%n", data.getMargemEbitda()));
        table.append(String.format("│ LPA            │ %10s │%n", data.getLpa()));
        table.append(String.format("│ Cresc.Rec 5a   │ %10s │%n", data.getCrescimentoReceita5a()));
        table.append("└────────────────┴────────────┘\n");
        table.append("```\n");
        return table.toString();
    }

    /**
     * Formata a tabela de indicadores para uma ação americana (US Stock).
     * Dividida em Valuation e Financial Metrics.
     *
     * @param ticker Código da ação
     * @param data   Indicadores da ação americana
     * @return String formatada com a tabela
     */
    private String formatUSStockTable(String ticker, USStockData data) {
        StringBuilder table = new StringBuilder();
        table.append("🇺🇸 *$").append(ticker).append("*\n");
        table.append("```\n");
        table.append("┌────────────────┬────────────┐\n");
        table.append("│  VALUATION     │            │\n");
        table.append("├────────────────┼────────────┤\n");
        table.append(String.format("│ P/E            │ %10s │%n", data.getPe()));
        table.append(String.format("│ P/S            │ %10s │%n", data.getPs()));
        table.append(String.format("│ EV/EBITDA      │ %10s │%n", data.getEvEbitda()));
        table.append(String.format("│ P/BV           │ %10s │%n", data.getPbv()));
        table.append(String.format("│ PEG            │ %10s │%n", data.getPeg()));
        table.append(String.format("│ FCF Yield      │ %10s │%n", data.getFcfYield()));
        table.append("├────────────────┼────────────┤\n");
        table.append("│  FINANCIALS    │            │\n");
        table.append("├────────────────┼────────────┤\n");
        table.append(String.format("│ ROE            │ %10s │%n", data.getRoe()));
        table.append(String.format("│ Marg. EBITDA   │ %10s │%n", data.getMargemEbitda()));
        table.append(String.format("│ Debt/EBITDA    │ %10s │%n", data.getDebtEbitda()));
        table.append(String.format("│ Current Ratio  │ %10s │%n", data.getCurrentRatio()));
        table.append(String.format("│ Div. Yield     │ %10s │%n", data.getDividendYield()));
        table.append(String.format("│ Payout Ratio   │ %10s │%n", data.getPayoutRatio()));
        table.append("└────────────────┴────────────┘\n");
        table.append("```\n");
        return table.toString();
    }

    /**
     * Formata a tabela de indicadores para uma criptomoeda.
     *
     * @param symbol Símbolo da criptomoeda
     * @param data   Indicadores da criptomoeda
     * @return String formatada com a tabela
     */
    private String formatCryptoTable(String symbol, CryptoData data) {
        StringBuilder table = new StringBuilder();
        table.append("🪙 *").append(symbol).append("* (" + escapeMarkdown(data.getName()) + ")\n");
        table.append("```\n");
        table.append("┌──────────────┬────────────────┐\n");
        table.append(String.format("│ Preço        │ %14s │%n", data.getPrice()));
        table.append(String.format("│ Market Cap   │ %14s │%n", data.getMarketCap()));
        table.append(String.format("│ Ranking      │ %14s │%n", "#" + data.getRank()));
        table.append("└──────────────┴────────────────┘\n");
        table.append("```\n");
        return table.toString();
    }

    /**
     * Envia mensagem de boas-vindas para novos usuários.
     */
    private void sendWelcomeMessage(long chatId) {
        String welcomeMessage = """
                🤖 *Bem-vindo ao Telegram Ticker Bot!*
                
                Este bot busca indicadores e notícias recentes sobre:
                • *Ações BR (Ibovespa)* — P/L, DY, P/VP, ROE, Dív/EBITDA, LPA
                • *US Stocks* — P/E, P/S, EV/EBITDA, PEG, ROE, FCF Yield
                • *Fundos Imobiliários* — DY, P/VP, Cotistas, Imóveis
                • *Criptomoedas* — Preço, Market Cap, Ranking
                
                *Como usar:*
                Envie os códigos separados por vírgulas.
                Use `$` antes de tickers americanos.
                
                *Exemplos:*
                `PETR3, VALE3, ITUB4` — Ações BR
                `$AAPL, $MSFT, $GOOGL` — US Stocks
                `HGLG11, KNRI11, MXRF11` — Fundos Imobiliários
                `BTC, ETH, SOL` — Criptomoedas
                `PETR3, $AAPL, HGLG11, BTC` — Misto
                
                📰 Fonte: Google News
                📊 Até 3 notícias por ativo (último mês)
                
                ─────────────────────────
                👨‍💻 *Criado por* — Fernando Almeida
                🐙 [GitHub](https://github.com/almeidaah)
                𝕏  [X/Twitter](https://x.com/faflpx)
                """;

        sendMessage(chatId, welcomeMessage);
    }

    /**
     * Envia mensagem quando o usuário envia mais ativos do que o permitido.
     */
    private void sendTooManyAssetsMessage(long chatId, int totalAssets) {
        String message = "⚠️ *Ops! Muitos ativos de uma vez.*\n\n"
                + "Você enviou *" + totalAssets + " ativos*, mas o limite é de *"
                + MAX_ASSETS_PER_REQUEST + " por mensagem*.\n\n"
                + "Por favor, envie até " + MAX_ASSETS_PER_REQUEST
                + " ativos por vez. \uD83D\uDE09\n\n"
                + "*Exemplo:* `HGLG11, PETR3, $AAPL`";

        sendMessage(chatId, message);
    }

    /**
     * Envia mensagem de instrução quando o usuário envia um comando inválido.
     */
    private void sendInstructionMessage(long chatId) {
        String instructionMessage = """
                ℹ️ *Formato inválido*
                
                Envie os códigos dos ativos separados por vírgulas.
                Use `$` antes de tickers americanos.
                
                *Exemplos:*
                `PETR3, VALE3, ITUB4` — Ações BR
                `$AAPL, $MSFT, $GOOGL` — US Stocks
                `HGLG11, KNRI11, MXRF11` — Fundos Imobiliários
                `BTC, ETH, SOL, ADA` — Criptomoedas
                `PETR3, $TSLA, HGLG11, BTC` — Misto
                
                *Dica:* Ações BR = 4 letras + 1 número (PETR3). US Stocks = $TICKER ($AAPL). Fundos = 4 letras + 2 números (HGLG11). Crypto = símbolo (BTC, ETH).
                """;

        sendMessage(chatId, instructionMessage);
    }

    /**
     * Envia uma mensagem para o chat especificado.
     *
     * @param chatId ID do chat de destino
     * @param text Texto da mensagem (suporta Markdown)
     */
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setDisableWebPagePreview(true);

        try {
            execute(message);
            System.out.println("✉️ Mensagem enviada para chat: " + chatId);
        } catch (TelegramApiException e) {
            System.err.println("❌ Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    /**
     * Escapa caracteres especiais do Markdown para evitar erros de formatação.
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("`", "\\`");
    }
}





