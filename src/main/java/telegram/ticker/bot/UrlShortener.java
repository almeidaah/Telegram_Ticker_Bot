package telegram.ticker.bot;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Utilitário para encurtar URLs usando a API gratuita do TinyURL.
 *
 * API: https://tinyurl.com/api-create.php?url={url}
 *
 * Em caso de falha, retorna a URL original sem encurtar.
 */
public class UrlShortener {

    private static final String TINYURL_API = "https://tinyurl.com/api-create.php?url=";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    public UrlShortener() {
        this(false);
    }

    public UrlShortener(boolean disableSslVerification) {
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
    }

    // Package-private constructor for testing
    UrlShortener(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Encurta uma URL usando o TinyURL.
     * Em caso de falha, retorna a URL original.
     *
     * @param longUrl URL longa para encurtar
     * @return URL encurtada ou a URL original se falhar
     */
    public String shorten(String longUrl) {
        if (longUrl == null || longUrl.isEmpty()) {
            return longUrl;
        }

        try {
            String encodedUrl = URLEncoder.encode(longUrl, StandardCharsets.UTF_8);
            String apiUrl = TINYURL_API + encodedUrl;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String shortUrl = response.body().trim();
                if (shortUrl.startsWith("https://")) {
                    return shortUrl;
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
