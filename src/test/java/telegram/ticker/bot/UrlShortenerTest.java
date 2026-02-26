package telegram.ticker.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link UrlShortener} usando a API v2 do TinyURL.
 * Utiliza {@link StubHttpClient} para simular respostas HTTP sem rede.
 */
class UrlShortenerTest {

    private static final String FAKE_TOKEN = "test-api-token-123";

    // ── Cenários de sucesso ───────────────────────────────────

    @Test
    void shouldReturnShortenedUrlOnSuccess() {
        String jsonResponse = """
                {
                  "data": {
                    "domain": "tinyurl.com",
                    "alias": "abc123",
                    "url": "https://example.com/very/long/url",
                    "tiny_url": "https://tinyurl.com/abc123"
                  },
                  "code": 0,
                  "errors": []
                }
                """;

        StubHttpClient stub = new StubHttpClient().withResponse(200, jsonResponse);
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String result = shortener.shorten("https://example.com/very/long/url");
        assertEquals("https://tinyurl.com/abc123", result);
    }

    @Test
    void shouldHandleEscapedSlashesInResponse() {
        // Some JSON serializers escape forward slashes
        String jsonResponse = """
                {"data":{"tiny_url":"https:\\/\\/tinyurl.com\\/xyz789"},"code":0}
                """;

        StubHttpClient stub = new StubHttpClient().withResponse(200, jsonResponse);
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String result = shortener.shorten("https://example.com/test");
        assertEquals("https://tinyurl.com/xyz789", result);
    }

    @Test
    void shouldSendPostRequestWithCorrectHeaders() {
        String jsonResponse = """
                {"data":{"tiny_url":"https://tinyurl.com/ok"},"code":0}
                """;

        StubHttpClient stub = new StubHttpClient().withResponse(200, jsonResponse);
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        shortener.shorten("https://example.com");

        // Verify the request was a POST to the correct endpoint
        assertNotNull(stub.getLastRequest());
        assertEquals("POST", stub.getLastRequest().method());
        assertEquals("https://api.tinyurl.com/create", stub.getLastRequest().uri().toString());

        // Verify Authorization header contains the token
        var authHeader = stub.getLastRequest().headers().firstValue("Authorization");
        assertTrue(authHeader.isPresent());
        assertEquals("Bearer " + FAKE_TOKEN, authHeader.get());

        // Verify Content-Type is JSON
        var contentType = stub.getLastRequest().headers().firstValue("Content-Type");
        assertTrue(contentType.isPresent());
        assertEquals("application/json", contentType.get());
    }

    // ── Cenários de falha ─────────────────────────────────────

    @Test
    void shouldReturnOriginalUrlOnNon200Status() {
        StubHttpClient stub = new StubHttpClient().withResponse(401, "Unauthorized");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/long";
        assertEquals(original, shortener.shorten(original));
    }

    @Test
    void shouldReturnOriginalUrlOnInvalidJsonResponse() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "not valid json");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/long";
        assertEquals(original, shortener.shorten(original));
    }

    @Test
    void shouldReturnOriginalUrlOnEmptyBody() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/long";
        assertEquals(original, shortener.shorten(original));
    }

    @Test
    void shouldReturnOriginalUrlOnIOException() {
        StubHttpClient stub = new StubHttpClient().withIOException("Connection refused");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/long";
        assertEquals(original, shortener.shorten(original));
    }

    @Test
    void shouldReturnOriginalUrlOnInterruptedException() {
        StubHttpClient stub = new StubHttpClient().withInterruptedException("Interrupted");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/long";
        assertEquals(original, shortener.shorten(original));
    }

    @Test
    void shouldReturnOriginalUrlOn500Error() {
        StubHttpClient stub = new StubHttpClient().withResponse(500, "Internal Server Error");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/test";
        assertEquals(original, shortener.shorten(original));
    }

    // ── Cenários de token ausente ─────────────────────────────

    @Test
    void shouldReturnOriginalUrlWhenTokenIsNull() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "ignored");
        UrlShortener shortener = new UrlShortener(stub, null);

        String original = "https://example.com/no-token";
        assertEquals(original, shortener.shorten(original));

        // Should NOT have made any HTTP call
        assertNull(stub.getLastRequest());
    }

    @Test
    void shouldReturnOriginalUrlWhenTokenIsEmpty() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "ignored");
        UrlShortener shortener = new UrlShortener(stub, "");

        String original = "https://example.com/empty-token";
        assertEquals(original, shortener.shorten(original));

        // Should NOT have made any HTTP call
        assertNull(stub.getLastRequest());
    }

    // ── Cenários de entrada nula/vazia ────────────────────────

    @Test
    void shouldReturnNullForNullInput() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "ignored");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        assertNull(shortener.shorten(null));
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        StubHttpClient stub = new StubHttpClient().withResponse(200, "ignored");
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        assertEquals("", shortener.shorten(""));
    }

    // ── Cenário com URL contendo caracteres especiais ─────────

    @Test
    void shouldHandleUrlWithSpecialCharacters() {
        String jsonResponse = """
                {"data":{"tiny_url":"https://tinyurl.com/special"},"code":0}
                """;

        StubHttpClient stub = new StubHttpClient().withResponse(200, jsonResponse);
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String result = shortener.shorten("https://example.com/search?q=hello&lang=pt-BR");
        assertEquals("https://tinyurl.com/special", result);
    }

    @Test
    void shouldReturnOriginalUrlWhenJsonMissingTinyUrl() {
        String jsonResponse = """
                {"data":{"domain":"tinyurl.com"},"code":0}
                """;

        StubHttpClient stub = new StubHttpClient().withResponse(200, jsonResponse);
        UrlShortener shortener = new UrlShortener(stub, FAKE_TOKEN);

        String original = "https://example.com/missing-field";
        assertEquals(original, shortener.shorten(original));
    }
}
