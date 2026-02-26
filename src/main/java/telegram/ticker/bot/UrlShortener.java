package telegram.ticker.bot;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para encurtar URLs usando a API v2 do TinyURL.
 *
 * API: POST https://api.tinyurl.com/create
 * Requer token de autenticação (variável de ambiente TINYURL_API_TOKEN).
 *
 * Em caso de falha ou token ausente, retorna a URL original sem encurtar.
 */
public class UrlShortener {

    private static final String TINYURL_API = "https://api.tinyurl.com/create";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    /** Regex para extrair o campo "tiny_url" do JSON de resposta. */
    private static final Pattern TINY_URL_PATTERN =
            Pattern.compile("\"tiny_url\"\\s*:\\s*\"(https?://[^\"]+)\"");

    private final HttpClient httpClient;
    private final String apiToken;

    public UrlShortener() {
        this(false);
    }

    public UrlShortener(boolean disableSslVerification) {
        this(disableSslVerification, System.getenv("TINYURL_API_TOKEN"));
    }

    public UrlShortener(boolean disableSslVerification, String apiToken) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL no UrlShortener: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
        this.apiToken = apiToken;
    }

    // Package-private constructor for testing
    UrlShortener(HttpClient httpClient, String apiToken) {
        this.httpClient = httpClient;
        this.apiToken = apiToken;
    }

    /**
     * Encurta uma URL usando o TinyURL API v2.
     * Em caso de falha ou token ausente, retorna a URL original.
     *
     * @param longUrl URL longa para encurtar
     * @return URL encurtada ou a URL original se falhar
     */
    public String shorten(String longUrl) {
        if (longUrl == null || longUrl.isEmpty()) {
            return longUrl;
        }

        if (apiToken == null || apiToken.isEmpty()) {
            System.err.println("⚠️ TINYURL_API_TOKEN não configurado — URL não será encurtada.");
            return longUrl;
        }

        try {
            // Escapa aspas e barras dentro da URL para o JSON
            String escapedUrl = longUrl.replace("\\", "\\\\").replace("\"", "\\\"");
            String jsonBody = "{\"url\":\"" + escapedUrl + "\",\"domain\":\"tinyurl.com\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TINYURL_API))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiToken)
                    .header("User-Agent", "TelegramTickerBot/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Unescape JSON-escaped forward slashes before matching
                String body = response.body().replace("\\/", "/");
                Matcher matcher = TINY_URL_PATTERN.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            // Silenciosamente retorna a URL original
            System.err.println("⚠️ Erro ao encurtar URL: " + e.getMessage());
        }

        return longUrl;
    }

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
