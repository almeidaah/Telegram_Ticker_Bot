package telegram.ticker.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Classe utilitária para buscar indicadores financeiros de ações americanas (US Stocks).
 *
 * Usa a API do Yahoo Finance (quoteSummary) com autenticação via crumb/cookie para obter:
 *
 * Valuation:
 * - P/E (trailingPE)
 * - P/S (priceToSalesTrailing12Months)
 * - EV/EBITDA (enterpriseToEbitda)
 * - P/BV (priceToBook)
 * - PEG (pegRatio)
 * - FCF Yield (freeCashflow / marketCap)
 *
 * Métricas Financeiras:
 * - ROE (returnOnEquity)
 * - Margem EBITDA (ebitdaMargins)
 * - Dívida/EBITDA (totalDebt / ebitda)
 * - Current Ratio (currentRatio)
 * - Dividend Yield (dividendYield)
 * - Payout Ratio (payoutRatio)
 *
 * API: https://query2.finance.yahoo.com/v10/finance/quoteSummary/{ticker}
 */
public class USStockDataFetcher {

    private static final String YAHOO_COOKIE_URL = "https://fc.yahoo.com";
    private static final String YAHOO_CRUMB_URL = "https://query2.finance.yahoo.com/v1/test/getcrumb";
    private static final String YAHOO_QUOTE_SUMMARY_URL =
            "https://query2.finance.yahoo.com/v10/finance/quoteSummary/";
    private static final String MODULES =
            "?modules=defaultKeyStatistics,financialData,summaryDetail";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Cached crumb (refreshed when expired/invalid)
    private volatile String cachedCrumb;

    public USStockDataFetcher() {
        this(false);
    }

    /**
     * Construtor com opção de SSL permissivo.
     *
     * @param disableSslVerification Se true, desabilita verificação de certificados SSL
     */
    public USStockDataFetcher(boolean disableSslVerification) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookieManager);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL permissivo no USStockDataFetcher: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    // Package-private constructor for testing
    USStockDataFetcher(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        // Pre-set a dummy crumb for tests to skip auth handshake
        this.cachedCrumb = "test-crumb";
    }

    /**
     * Obtém cookie + crumb do Yahoo Finance para autenticação.
     * O cookie é gerenciado automaticamente pelo CookieManager do HttpClient.
     *
     * @return O crumb string, ou null se falhar
     */
    private String fetchCrumb() {
        try {
            // Step 1: Hit fc.yahoo.com to get cookies
            HttpRequest cookieRequest = HttpRequest.newBuilder()
                    .uri(URI.create(YAHOO_COOKIE_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            httpClient.send(cookieRequest, HttpResponse.BodyHandlers.ofString());

            // Step 2: Get crumb using the cookies
            HttpRequest crumbRequest = HttpRequest.newBuilder()
                    .uri(URI.create(YAHOO_CRUMB_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> crumbResponse = httpClient.send(crumbRequest, HttpResponse.BodyHandlers.ofString());

            if (crumbResponse.statusCode() == 200) {
                String crumb = crumbResponse.body().trim();
                if (!crumb.isEmpty()) {
                    System.out.println("🔑 Yahoo Finance crumb obtido com sucesso");
                    return crumb;
                }
            }
            System.err.println("⚠️ Falha ao obter crumb do Yahoo Finance (HTTP " + crumbResponse.statusCode() + ")");
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao obter crumb do Yahoo Finance: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retorna o crumb em cache ou busca um novo.
     */
    private String getCrumb() {
        if (cachedCrumb == null) {
            cachedCrumb = fetchCrumb();
        }
        return cachedCrumb;
    }

    /**
     * Busca indicadores financeiros de uma ação americana.
     *
     * @param ticker Código da ação (ex: AAPL, MSFT, GOOGL, TSLA)
     * @return USStockData com os indicadores encontrados
     */
    public USStockData fetchData(String ticker) {
        String upperTicker = ticker.toUpperCase();

        try {
            String crumb = getCrumb();
            if (crumb == null) {
                System.err.println("⚠️ Yahoo Finance - Sem crumb, não é possível autenticar para " + upperTicker);
                return emptyData(upperTicker);
            }

            String url = YAHOO_QUOTE_SUMMARY_URL + upperTicker + MODULES + "&crumb=" + crumb;
            System.out.println("🇺🇸 Buscando indicadores US: " + YAHOO_QUOTE_SUMMARY_URL + upperTicker);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // If 401, crumb may have expired — retry once
            if (response.statusCode() == 401) {
                System.out.println("🔄 Crumb expirado, renovando...");
                cachedCrumb = null;
                crumb = getCrumb();
                if (crumb == null) {
                    return emptyData(upperTicker);
                }
                url = YAHOO_QUOTE_SUMMARY_URL + upperTicker + MODULES + "&crumb=" + crumb;
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                System.err.println("⚠️ Yahoo Finance - Status HTTP: " + response.statusCode() + " para " + upperTicker);
                return emptyData(upperTicker);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.path("quoteSummary").path("result");

            if (!result.isArray() || result.isEmpty()) {
                System.err.println("⚠️ Yahoo Finance - Sem resultado para " + upperTicker);
                return emptyData(upperTicker);
            }

            JsonNode data = result.get(0);
            JsonNode keyStats = data.path("defaultKeyStatistics");
            JsonNode financial = data.path("financialData");
            JsonNode summary = data.path("summaryDetail");

            // Nome da empresa (não disponível neste endpoint, usar ticker)
            String name = upperTicker;

            // === Valuation ===
            String pe = formatRawValue(summary.path("trailingPE"), "x");
            String ps = formatRawValue(summary.path("priceToSalesTrailing12Months"), "x");
            String evEbitda = formatRawValue(keyStats.path("enterpriseToEbitda"), "x");
            String pbv = formatRawValue(keyStats.path("priceToBook"), "x");
            String peg = formatRawValue(keyStats.path("pegRatio"), "x");

            // FCF Yield = freeCashflow / marketCap
            String fcfYield = calculateFcfYield(financial, summary);

            // === Financial Metrics ===
            String roe = formatPercentValue(financial.path("returnOnEquity"));
            String margemEbitda = formatPercentValue(financial.path("ebitdaMargins"));

            // Debt/EBITDA = totalDebt / ebitda
            String debtEbitda = calculateDebtEbitda(financial);

            String currentRatio = formatRawValue(financial.path("currentRatio"), "x");
            String dividendYield = formatPercentValue(summary.path("dividendYield"));
            String payoutRatio = formatPercentValue(summary.path("payoutRatio"));

            System.out.println("📊 " + upperTicker + " → P/E: " + pe + " | P/S: " + ps
                    + " | EV/EBITDA: " + evEbitda + " | P/BV: " + pbv + " | PEG: " + peg
                    + " | FCF Yield: " + fcfYield + " | ROE: " + roe + " | Marg.EBITDA: " + margemEbitda
                    + " | Debt/EBITDA: " + debtEbitda + " | CurrRatio: " + currentRatio
                    + " | DY: " + dividendYield + " | Payout: " + payoutRatio);

            return new USStockData(upperTicker, name, pe, ps, evEbitda, pbv, peg, fcfYield,
                    roe, margemEbitda, debtEbitda, currentRatio, dividendYield, payoutRatio);

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro ao buscar dados de " + upperTicker + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar dados de " + upperTicker + ": " + e.getMessage());
        }

        return emptyData(upperTicker);
    }

    /**
     * Extrai o valor "raw" de um nó JSON do Yahoo Finance e formata.
     * A estrutura típica é: {"raw": 28.45, "fmt": "28.45"}
     *
     * @param node   Nó JSON do campo
     * @param suffix Sufixo para o valor (ex: "x", "%")
     * @return Valor formatado ou "N/A"
     */
    private String formatRawValue(JsonNode node, String suffix) {
        if (node == null || node.isMissingNode() || node.isNull()) return "N/A";

        // Tenta "fmt" primeiro (valor já formatado pelo Yahoo)
        if (node.has("fmt") && !node.get("fmt").isNull()) {
            String fmt = node.get("fmt").asText();
            if (fmt != null && !fmt.isEmpty() && !fmt.equals("None")) {
                return fmt;
            }
        }

        // Fallback para "raw"
        if (node.has("raw")) {
            double raw = node.get("raw").asDouble();
            return String.format("%.2f%s", raw, suffix != null ? suffix : "");
        }

        return "N/A";
    }

    /**
     * Formata um valor como porcentagem.
     * O Yahoo Finance retorna valores como 0.2345 (= 23.45%).
     */
    private String formatPercentValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "N/A";

        // Tenta "fmt" primeiro
        if (node.has("fmt") && !node.get("fmt").isNull()) {
            String fmt = node.get("fmt").asText();
            if (fmt != null && !fmt.isEmpty() && !fmt.equals("None")) {
                return fmt;
            }
        }

        // Fallback para "raw" (converter para %)
        if (node.has("raw")) {
            double raw = node.get("raw").asDouble();
            return String.format("%.2f%%", raw * 100);
        }

        return "N/A";
    }

    /**
     * Calcula FCF Yield = Free Cash Flow / Market Cap.
     */
    private String calculateFcfYield(JsonNode financial, JsonNode summary) {
        try {
            double fcf = extractRaw(financial.path("freeCashflow"));
            double marketCap = extractRaw(summary.path("marketCap"));

            if (fcf != 0 && marketCap != 0) {
                double yield = (fcf / marketCap) * 100;
                return String.format("%.2f%%", yield);
            }
        } catch (Exception e) {
            // Silenciar
        }
        return "N/A";
    }

    /**
     * Calcula Debt/EBITDA = Total Debt / EBITDA.
     */
    private String calculateDebtEbitda(JsonNode financial) {
        try {
            double totalDebt = extractRaw(financial.path("totalDebt"));
            double ebitda = extractRaw(financial.path("ebitda"));

            if (totalDebt != 0 && ebitda != 0) {
                double ratio = totalDebt / ebitda;
                return String.format("%.2fx", ratio);
            }
        } catch (Exception e) {
            // Silenciar
        }
        return "N/A";
    }

    /**
     * Extrai o valor numérico raw de um nó JSON.
     */
    private double extractRaw(JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull() && node.has("raw")) {
            return node.get("raw").asDouble();
        }
        return 0;
    }

    /**
     * Retorna um USStockData com todos os valores como "N/A".
     */
    private USStockData emptyData(String ticker) {
        return new USStockData(ticker, ticker,
                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A",
                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A");
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
