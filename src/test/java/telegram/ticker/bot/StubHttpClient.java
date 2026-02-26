package telegram.ticker.bot;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Stub HttpClient para testes unitários.
 * Permite configurar uma resposta fixa (status code + body) sem necessidade de rede.
 */
class StubHttpClient extends HttpClient {

    private int statusCode = 200;
    private String responseBody = "";
    private IOException ioException;
    private InterruptedException interruptedException;
    private HttpRequest lastRequest;

    StubHttpClient withResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.responseBody = body;
        this.ioException = null;
        this.interruptedException = null;
        return this;
    }

    StubHttpClient withIOException(String message) {
        this.ioException = new IOException(message);
        return this;
    }

    StubHttpClient withInterruptedException(String message) {
        this.interruptedException = new InterruptedException(message);
        return this;
    }

    HttpRequest getLastRequest() {
        return lastRequest;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        this.lastRequest = request;

        if (ioException != null) throw ioException;
        if (interruptedException != null) throw interruptedException;

        return (HttpResponse<T>) new StubHttpResponse(statusCode, responseBody, request);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler) {
        throw new UnsupportedOperationException("Not implemented for tests");
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        throw new UnsupportedOperationException("Not implemented for tests");
    }

    @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
    @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
    @Override public Redirect followRedirects() { return Redirect.NORMAL; }
    @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
    @Override public SSLContext sslContext() { return null; }
    @Override public SSLParameters sslParameters() { return null; }
    @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
    @Override public Version version() { return Version.HTTP_2; }
    @Override public Optional<Executor> executor() { return Optional.empty(); }

    /**
     * Stub HttpResponse que retorna valores configurados.
     */
    private static class StubHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;
        private final HttpRequest request;

        StubHttpResponse(int statusCode, String body, HttpRequest request) {
            this.statusCode = statusCode;
            this.body = body;
            this.request = request;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public String body() { return body; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public java.net.URI uri() { return request.uri(); }
        @Override public Version version() { return Version.HTTP_2; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
    }
}
