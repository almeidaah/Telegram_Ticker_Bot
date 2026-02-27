package telegram.ticker.bot;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Encurtador de URLs usando a API pública do is.gd.
 *
 * O is.gd é um serviço gratuito de encurtamento de URLs que não requer
 * autenticação ou chave de API. A API retorna a URL encurtada como texto puro.
 *
 * Endpoint: https://is.gd/create.php?format=simple&url={encoded_url}
 *
 * Implementado com java.net.http.HttpClient nativo (sem bibliotecas externas).
 * Em caso de falha (rede, timeout, etc.), retorna a URL original como fallback.
 */
public class UrlShortener {

    private static final String ISGD_API_URL = "https://is.gd/create.php";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    /**
     * Construtor padrão — cria um HttpClient nativo.
     */
    public UrlShortener() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Construtor para injeção de HttpClient (usado em testes).
     *
     * @param httpClient cliente HTTP a ser utilizado
     */
    UrlShortener(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Encurta uma URL usando o serviço is.gd.
     *
     * Se a URL for nula retorna null. Se for vazia retorna vazio.
     * Em caso de qualquer erro (rede, status HTTP != 200, resposta vazia),
     * retorna a URL original como fallback para não quebrar o fluxo.
     *
     * @param longUrl URL original a ser encurtada
     * @return URL encurtada ou a URL original em caso de falha
     */
    public String shorten(String longUrl) {
        if (longUrl == null) {
            return null;
        }
        if (longUrl.isBlank()) {
            return longUrl;
        }

        try {
            String encoded = URLEncoder.encode(longUrl, StandardCharsets.UTF_8);
            String apiUrl = "%s?format=simple&url=%s".formatted(ISGD_API_URL, encoded);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "TelegramTickerBot/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String shortened = response.body();
                if (shortened != null && !shortened.isBlank()) {
                    return shortened.trim();
                }
            }

            System.err.println("⚠️ is.gd retornou status " + response.statusCode());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Requisição ao is.gd interrompida: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao encurtar URL via is.gd: " + e.getMessage());
        }

        // Fallback: retorna a URL original
        return longUrl;
    }
}
