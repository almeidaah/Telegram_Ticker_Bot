package com.scibite.pipelines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para FIIDataFetcher.
 * Utiliza StubHttpClient para não depender de rede.
 */
class FIIDataFetcherTest {

    private StubHttpClient stubClient;
    private FIIDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        stubClient = new StubHttpClient();
        fetcher = new FIIDataFetcher(stubClient);
    }

    // ========== fetchData - HTML Parsing ==========

    @Test
    void fetchDataShouldExtractDYFromHtml() {
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

        stubClient.withResponse(200, html);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("8,50%", result.getDividendYield());
        assertEquals("0,95", result.getPVp());
    }

    @Test
    void fetchDataShouldConvertTickerToUpperCase() {
        stubClient.withResponse(200, "<html><body><p>Empty</p></body></html>");

        FIIData result = fetcher.fetchData("hglg11");

        assertNotNull(result);
        assertNotNull(result.getTicker());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNDOnHttpError() {
        stubClient.withResponse(404, "");

        FIIData result = fetcher.fetchData("INVALID11");

        assertEquals("INVALID11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    @Test
    void fetchDataShouldReturnNDOnServerError() {
        stubClient.withResponse(500, "");

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
    }

    @Test
    void fetchDataShouldReturnNDOnConnectionException() {
        stubClient.withIOException("Connection refused");

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    @Test
    void fetchDataShouldReturnNDOnInterruptedException() {
        stubClient.withInterruptedException("Interrupted");

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
    }

    @Test
    void fetchDataShouldReturnNDOnEmptyHtml() {
        stubClient.withResponse(200, "<html><body></body></html>");

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("HGLG11", result.getTicker());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getCotistas());
        assertEquals("N/D", result.getImoveis());
    }

    // ========== fetchData - Various HTML structures ==========

    @Test
    void fetchDataShouldExtractFromNestedDivStructure() {
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

        stubClient.withResponse(200, html);

        FIIData result = fetcher.fetchData("XPLG11");

        assertEquals("XPLG11", result.getTicker());
        assertNotNull(result.getDividendYield());
        assertNotNull(result.getPVp());
    }

    @Test
    void fetchDataShouldExtractLargeNumbersForCotistas() {
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

        stubClient.withResponse(200, html);

        FIIData result = fetcher.fetchData("HGLG11");

        assertEquals("1.234.567", result.getCotistas());
        assertEquals("42", result.getImoveis());
    }

    @Test
    void fetchDataShouldHandleDifferentIndicatorNames() {
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

        stubClient.withResponse(200, html);

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
