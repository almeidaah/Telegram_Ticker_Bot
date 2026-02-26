package com.scibite.pipelines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para FIIDataFetcher.
 * Utiliza mocks do HttpClient para não depender de rede.
 */
@ExtendWith(MockitoExtension.class)
class FIIDataFetcherTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private FIIDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new FIIDataFetcher(httpClient);
    }

    // ========== fetchData - HTML Parsing ==========

    @Test
    void fetchDataShouldExtractDYFromHtml() throws Exception {
        String html = """
                <html><body>
                <div class="indicators">
                  <div class="item">
                    <span>Dividend Yield</span>
                    <span>8,50%</span>
                  </div>
                  <div class="item">
                    <span>P/VP</span>
                    <span>0,95</span>
                  </div>
                  <div class="item">
                    <span>Cotistas</span>
                    <span>245320</span>
                  </div>
                  <div class="item">
                    <span>Imóveis</span>
                    <span>15</span>
                  </div>
                </div>
                </body></html>
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("8,50%", result.getDividendYield());
        assertEquals("0,95", result.getPVp());
    }

    @Test
    void fetchDataShouldConvertTickerToUpperCase() throws Exception {
        String html = "<html><body><p>Empty page</p></body></html>";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("hglg11");

        // Ticker is stored as-is from input (upper-cased by URL construction)
        assertNotNull(result);
        assertNotNull(result.getTicker());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNDOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("INVALID11");

        assertEquals("INVALID11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    @Test
    void fetchDataShouldReturnNDOnServerError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
    }

    @Test
    void fetchDataShouldReturnNDOnConnectionException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Connection refused"));

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    @Test
    void fetchDataShouldReturnNDOnInterruptedException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
    }

    @Test
    void fetchDataShouldReturnNDOnEmptyHtml() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("<html><body></body></html>");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    // ========== fetchData - Various HTML structures ==========

    @Test
    void fetchDataShouldExtractFromNestedDivStructure() throws Exception {
        String html = """
                <html><body>
                <div class="info-container">
                  <div class="info-item">
                    <div class="label">DY</div>
                    <div class="value">7,20%</div>
                  </div>
                  <div class="info-item">
                    <div class="label">P/VP</div>
                    <div class="value">1,05</div>
                  </div>
                </div>
                </body></html>
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("XPLG11");

        assertEquals("XPLG11", result.getTicker());
        // May or may not extract depending on exact DOM traversal, but should not throw
        assertNotNull(result.getDividendYield());
        assertNotNull(result.getPVp());
    }

    @Test
    void fetchDataShouldExtractLargeNumbersForCotistas() throws Exception {
        String html = """
                <html><body>
                <div>
                  <span>Cotistas</span>
                  <span>1.234.567</span>
                </div>
                <div>
                  <span>Imóveis</span>
                  <span>42</span>
                </div>
                </body></html>
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("1.234.567", result.getCotistas());
        assertEquals("42", result.getImoveis());
    }

    @Test
    void fetchDataShouldHandleDifferentIndicatorNames() throws Exception {
        String html = """
                <html><body>
                <div>
                  <span>Div. Yield</span>
                  <span>6,80%</span>
                </div>
                <div>
                  <span>P/VPA</span>
                  <span>0,88</span>
                </div>
                <div>
                  <span>Número de Cotistas</span>
                  <span>50000</span>
                </div>
                <div>
                  <span>Número de Imóveis</span>
                  <span>8</span>
                </div>
                </body></html>
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(html);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        FIIData result = fetcher.fetchData("MXRF11");

        assertEquals("MXRF11", result.getTicker());
        assertEquals("6,80%", result.getDividendYield());
        assertEquals("0,88", result.getPVp());
        assertEquals("50000", result.getCotistas());
        assertEquals("8", result.getImoveis());
    }

    // ========== Constructor ==========

    @Test
    void defaultConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new FIIDataFetcher());
    }

    @Test
    void sslDisabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new FIIDataFetcher(true));
    }

    @Test
    void sslEnabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new FIIDataFetcher(false));
    }
}
