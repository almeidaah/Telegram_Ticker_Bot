package telegram.ticker.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitária para buscar indicadores de criptomoedas.
 *
 * Usa a API pública do CoinGecko para obter:
 * - Preço atual em USD
 * - Market Cap em USD
 * - Posição no ranking (market_cap_rank)
 *
 * API: https://api.coingecko.com/api/v3/coins/markets
 */
public class CryptoDataFetcher {

    private static final String COINGECKO_MARKETS_URL = "https://api.coingecko.com/api/v3/coins/markets";
    private static final String COINGECKO_SEARCH_URL = "https://api.coingecko.com/api/v3/search";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Mapeamento de símbolo -> CoinGecko ID para as principais criptomoedas
    private static final Map<String, String> SYMBOL_TO_ID = new HashMap<>();

    static {
        SYMBOL_TO_ID.put("BTC", "bitcoin");
        SYMBOL_TO_ID.put("ETH", "ethereum");
        SYMBOL_TO_ID.put("USDT", "tether");
        SYMBOL_TO_ID.put("BNB", "binancecoin");
        SYMBOL_TO_ID.put("SOL", "solana");
        SYMBOL_TO_ID.put("XRP", "ripple");
        SYMBOL_TO_ID.put("USDC", "usd-coin");
        SYMBOL_TO_ID.put("ADA", "cardano");
        SYMBOL_TO_ID.put("DOGE", "dogecoin");
        SYMBOL_TO_ID.put("TRX", "tron");
        SYMBOL_TO_ID.put("AVAX", "avalanche-2");
        SYMBOL_TO_ID.put("LINK", "chainlink");
        SYMBOL_TO_ID.put("DOT", "polkadot");
        SYMBOL_TO_ID.put("MATIC", "matic-network");
        SYMBOL_TO_ID.put("POL", "matic-network");
        SYMBOL_TO_ID.put("TON", "the-open-network");
        SYMBOL_TO_ID.put("SHIB", "shiba-inu");
        SYMBOL_TO_ID.put("DAI", "dai");
        SYMBOL_TO_ID.put("LTC", "litecoin");
        SYMBOL_TO_ID.put("BCH", "bitcoin-cash");
        SYMBOL_TO_ID.put("UNI", "uniswap");
        SYMBOL_TO_ID.put("ATOM", "cosmos");
        SYMBOL_TO_ID.put("XLM", "stellar");
        SYMBOL_TO_ID.put("ETC", "ethereum-classic");
        SYMBOL_TO_ID.put("NEAR", "near");
        SYMBOL_TO_ID.put("ICP", "internet-computer");
        SYMBOL_TO_ID.put("APT", "aptos");
        SYMBOL_TO_ID.put("FIL", "filecoin");
        SYMBOL_TO_ID.put("ALGO", "algorand");
        SYMBOL_TO_ID.put("XMR", "monero");
        SYMBOL_TO_ID.put("OP", "optimism");
        SYMBOL_TO_ID.put("ARB", "arbitrum");
        SYMBOL_TO_ID.put("SUI", "sui");
        SYMBOL_TO_ID.put("PEPE", "pepe");
        SYMBOL_TO_ID.put("VET", "vechain");
        SYMBOL_TO_ID.put("AAVE", "aave");
        SYMBOL_TO_ID.put("GRT", "the-graph");
        SYMBOL_TO_ID.put("MKR", "maker");
        SYMBOL_TO_ID.put("SAND", "the-sandbox");
        SYMBOL_TO_ID.put("MANA", "decentraland");
        SYMBOL_TO_ID.put("AXS", "axie-infinity");
        SYMBOL_TO_ID.put("FTM", "fantom");
        SYMBOL_TO_ID.put("THETA", "theta-token");
        SYMBOL_TO_ID.put("RUNE", "thorchain");
        SYMBOL_TO_ID.put("HBAR", "hedera-hashgraph");
        SYMBOL_TO_ID.put("INJ", "injective-protocol");
        SYMBOL_TO_ID.put("SEI", "sei-network");
        SYMBOL_TO_ID.put("STX", "blockstack");
        SYMBOL_TO_ID.put("IMX", "immutable-x");
        SYMBOL_TO_ID.put("RENDER", "render-token");
        SYMBOL_TO_ID.put("FET", "fetch-ai");
    }

    public CryptoDataFetcher() {
        this(false);
    }

    /**
     * Construtor com opção de SSL permissivo.
     *
     * @param disableSslVerification Se true, desabilita verificação de certificados SSL
     */
    public CryptoDataFetcher(boolean disableSslVerification) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL permissivo no CryptoDataFetcher: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    // Package-private constructor for testing
    CryptoDataFetcher(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifica se um símbolo é uma criptomoeda conhecida.
     *
     * @param symbol Símbolo em maiúsculas (ex: BTC)
     * @return true se for reconhecido como crypto
     */
    public static boolean isKnownCrypto(String symbol) {
        return SYMBOL_TO_ID.containsKey(symbol.toUpperCase());
    }

    /**
     * Busca indicadores de uma criptomoeda pelo símbolo.
     *
     * @param symbol Símbolo da criptomoeda (ex: BTC, ETH, SOL)
     * @return CryptoData com os indicadores encontrados
     */
    public CryptoData fetchData(String symbol) {
        String upperSymbol = symbol.toUpperCase();

        try {
            // Tenta resolver o CoinGecko ID
            String coinId = SYMBOL_TO_ID.get(upperSymbol);
            if (coinId == null) {
                coinId = searchCoinId(upperSymbol);
            }

            if (coinId == null) {
                System.err.println("⚠️ Criptomoeda não encontrada: " + upperSymbol);
                return new CryptoData(upperSymbol, upperSymbol, "N/D", "N/D", "N/D");
            }

            // Busca dados de mercado
            String url = String.format(
                "%s?vs_currency=usd&ids=%s&order=market_cap_desc&sparkline=false",
                COINGECKO_MARKETS_URL, coinId
            );

            System.out.println("🪙 Buscando crypto: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (compatible; FIINewsBot/1.0)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠️ CoinGecko - Status HTTP: " + response.statusCode() + " para " + upperSymbol);
                return new CryptoData(upperSymbol, upperSymbol, "N/D", "N/D", "N/D");
            }

            // Parse JSON
            JsonNode root = objectMapper.readTree(response.body());
            if (root.isArray() && root.size() > 0) {
                JsonNode coin = root.get(0);

                String name = coin.has("name") ? coin.get("name").asText() : upperSymbol;
                double currentPrice = coin.has("current_price") ? coin.get("current_price").asDouble() : 0;
                double marketCap = coin.has("market_cap") ? coin.get("market_cap").asDouble() : 0;
                int rank = coin.has("market_cap_rank") && !coin.get("market_cap_rank").isNull()
                        ? coin.get("market_cap_rank").asInt() : 0;

                String formattedPrice = formatPrice(currentPrice);
                String formattedMarketCap = formatMarketCap(marketCap);
                String formattedRank = rank > 0 ? "#" + rank : "N/D";

                System.out.println("🪙 " + upperSymbol + " → Preço: " + formattedPrice
                        + " | MCap: " + formattedMarketCap + " | Rank: " + formattedRank);

                return new CryptoData(upperSymbol, name, formattedPrice, formattedMarketCap, formattedRank);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro ao buscar dados de " + upperSymbol + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar dados de " + upperSymbol + ": " + e.getMessage());
        }

        return new CryptoData(upperSymbol, upperSymbol, "N/D", "N/D", "N/D");
    }

    /**
     * Busca o CoinGecko ID de uma criptomoeda não mapeada usando a API de busca.
     *
     * @param symbol Símbolo da criptomoeda
     * @return CoinGecko ID ou null se não encontrado
     */
    private String searchCoinId(String symbol) {
        try {
            String url = COINGECKO_SEARCH_URL + "?query=" + symbol;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (compatible; FIINewsBot/1.0)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode coins = root.get("coins");
                if (coins != null && coins.isArray()) {
                    for (JsonNode coin : coins) {
                        String coinSymbol = coin.has("symbol") ? coin.get("symbol").asText().toUpperCase() : "";
                        if (coinSymbol.equals(symbol)) {
                            String id = coin.get("id").asText();
                            // Cache para futuras buscas
                            SYMBOL_TO_ID.put(symbol, id);
                            System.out.println("🔍 Encontrado: " + symbol + " → " + id);
                            return id;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar ID da crypto " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Formata o preço em USD de forma legível.
     * Ex: 42150.50 → "$42,150.50", 0.00001234 → "$0.00001234"
     */
    private String formatPrice(double price) {
        if (price == 0) return "N/D";
        if (price >= 1.0) {
            return String.format("$%,.2f", price);
        } else {
            // Para preços muito baixos (memecoins), mostra mais casas decimais
            return String.format("$%.8f", price);
        }
    }

    /**
     * Formata o market cap de forma legível.
     * Ex: 820500000000 → "$820.5B", 1200000000 → "$1.2B", 500000000 → "$500M"
     */
    private String formatMarketCap(double marketCap) {
        if (marketCap == 0) return "N/D";
        if (marketCap >= 1_000_000_000_000.0) {
            return String.format("$%.1fT", marketCap / 1_000_000_000_000.0);
        } else if (marketCap >= 1_000_000_000.0) {
            return String.format("$%.1fB", marketCap / 1_000_000_000.0);
        } else if (marketCap >= 1_000_000.0) {
            return String.format("$%.1fM", marketCap / 1_000_000.0);
        } else {
            return String.format("$%,.0f", marketCap);
        }
    }

    /**
     * Retorna o nome completo de uma criptomoeda a partir de um CryptoData já carregado,
     * ou o símbolo caso não tenha sido carregado ainda.
     */
    public String getCryptoName(String symbol) {
        String id = SYMBOL_TO_ID.get(symbol.toUpperCase());
        return id != null ? id : symbol;
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
