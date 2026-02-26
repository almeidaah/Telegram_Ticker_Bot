package telegram.ticker.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o UrlShortener (is.gd).
 * Usa StubHttpClient para simular respostas sem acesso à rede.
 */
class UrlShortenerTest {

    private StubHttpClient stubClient;
    private UrlShortener shortener;

    @BeforeEach
    void setUp() {
        stubClient = new StubHttpClient();
        shortener = new UrlShortener(stubClient);
    }

    // ── Cenários de sucesso ──────────────────────────────────────

    @Test
    void shouldReturnShortenedUrlOnSuccess() {
        stubClient.withResponse(200, "https://is.gd/abc123");

        String result = shortener.shorten("https://example.com/some/long/url");

        assertEquals("https://is.gd/abc123", result);
    }

    @Test
    void shouldTrimResponseBody() {
        stubClient.withResponse(200, "  https://is.gd/xyz789  \n");

        String result = shortener.shorten("https://example.com");

        assertEquals("https://is.gd/xyz789", result);
    }

    @Test
    void shouldSendGetRequestToIsgdApi() {
        stubClient.withResponse(200, "https://is.gd/ok");

        shortener.shorten("https://example.com");

        assertNotNull(stubClient.getLastRequest());
        String uri = stubClient.getLastRequest().uri().toString();
        assertTrue(uri.startsWith("https://is.gd/create.php?format=simple&url="));
        assertTrue(uri.contains("example.com"));
        assertEquals("GET", stubClient.getLastRequest().method());
    }

    @Test
    void shouldHandleUrlWithSpecialCharacters() {
        stubClient.withResponse(200, "https://is.gd/sp3c1al");

        String result = shortener.shorten("https://example.com/path?q=hello world&lang=pt-BR");

        assertEquals("https://is.gd/sp3c1al", result);
        // Verifica que a URL foi encoded corretamente
        String uri = stubClient.getLastRequest().uri().toString();
        assertTrue(uri.contains("hello"));
    }

    // ── Cenários de fallback (retorna URL original) ──────────────

    @Test
    void shouldReturnOriginalUrlOn500Error() {
        stubClient.withResponse(500, "Internal Server Error");

        String original = "https://example.com/article";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    @Test
    void shouldReturnOriginalUrlOn429RateLimit() {
        stubClient.withResponse(429, "Rate limit exceeded");

        String original = "https://example.com/news";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    @Test
    void shouldReturnOriginalUrlOnEmptyBody() {
        stubClient.withResponse(200, "");

        String original = "https://example.com/page";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    @Test
    void shouldReturnOriginalUrlOnBlankBody() {
        stubClient.withResponse(200, "   ");

        String original = "https://example.com/blank";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    @Test
    void shouldReturnOriginalUrlOnIOException() {
        stubClient.withIOException("Connection refused");

        String original = "https://example.com/fail";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    @Test
    void shouldReturnOriginalUrlOnInterruptedException() {
        stubClient.withInterruptedException("Thread interrupted");

        String original = "https://example.com/interrupted";
        String result = shortener.shorten(original);

        assertEquals(original, result);
    }

    // ── Cenários de entrada nula/vazia ───────────────────────────

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(shortener.shorten(null));
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        assertEquals("", shortener.shorten(""));
    }

    @Test
    void shouldReturnBlankForBlankInput() {
        assertEquals("   ", shortener.shorten("   "));
    }

    // ── Verificação de headers ───────────────────────────────────

    @Test
    void shouldIncludeUserAgentHeader() {
        stubClient.withResponse(200, "https://is.gd/ua");

        shortener.shorten("https://example.com");

        var headers = stubClient.getLastRequest().headers();
        assertTrue(headers.firstValue("User-Agent").isPresent());
        assertEquals("TelegramTickerBot/1.0", headers.firstValue("User-Agent").get());
    }
}
