package com.scibite.pipelines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para StockDataFetcher (ações brasileiras / Ibovespa).
 * Utiliza StubHttpClient para não depender de rede.
 */
class StockDataFetcherTest {

    private StubHttpClient stubClient;
    private StockDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        stubClient = new StubHttpClient();
        fetcher = new StockDataFetcher(stubClient);
    }

    // ========== fetchData - HTML Parsing (StatusInvest structure) ==========

    @Test
    void fetchDataShouldExtractIndicatorsFromStatusInvestStructure() {
        String html = """
                <html><body>
                <div class="info">
                  <h3 class="title"><span>P/L</span></h3>
                  <div class="value"><strong class="value">5,20</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>DY</span></h3>
                  <div class="value"><strong class="value">12,50%</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>P/VP</span></h3>
                  <div class="value"><strong class="value">1,10</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>ROE</span></h3>
                  <div class="value"><strong class="value">25,30%</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>LPA</span></h3>
                  <div class="value"><strong class="value">6,50</strong></div>
                </div>
                </body></html>
                """;

        stubClient.withResponse(200, html);

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("PETR3", result.getTicker());
        assertEquals("5,20", result.getPl());
        assertEquals("12,50%", result.getDividendYield());
        assertEquals("1,10", result.getPVp());
        assertEquals("25,30%", result.getRoe());
        assertEquals("6,50", result.getLpa());
    }

    @Test
    void fetchDataShouldHandleAlternativeIndicatorNames() {
        String html = """
                <html><body>
                <div class="info">
                  <h3 class="title"><span>DIV. YIELD</span></h3>
                  <div class="value"><strong class="value">8,00%</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>P/VPA</span></h3>
                  <div class="value"><strong class="value">0,90</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>MARG. EBITDA</span></h3>
                  <div class="value"><strong class="value">35,00%</strong></div>
                </div>
                </body></html>
                """;

        stubClient.withResponse(200, html);

        StockData result = fetcher.fetchData("VALE3");

        assertEquals("VALE3", result.getTicker());
        assertEquals("8,00%", result.getDividendYield());
        assertEquals("0,90", result.getPVp());
        assertEquals("35,00%", result.getMargemEbitda());
    }

    @Test
    void fetchDataShouldPreserveTicker() {
        stubClient.withResponse(200, "<html><body></body></html>");

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("PETR3", result.getTicker());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNDOnHttpError() {
        stubClient.withResponse(404, "");

        StockData result = fetcher.fetchData("INVALID3");

        assertEquals("INVALID3", result.getTicker());
        assertEquals("N/D", result.getPl());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getRoe());
        assertEquals("N/D", result.getDividaLiquidaEbitda());
        assertEquals("N/D", result.getMargemEbitda());
        assertEquals("N/D", result.getLpa());
        assertEquals("N/D", result.getCrescimentoReceita5a());
    }

    @Test
    void fetchDataShouldReturnNDOnServerError() {
        stubClient.withResponse(500, "");

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("N/D", result.getPl());
        assertEquals("N/D", result.getDividendYield());
    }

    @Test
    void fetchDataShouldReturnNDOnIOException() {
        stubClient.withIOException("Timeout");

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("PETR3", result.getTicker());
        assertEquals("N/D", result.getPl());
        assertEquals("N/D", result.getDividendYield());
    }

    @Test
    void fetchDataShouldReturnNDOnInterruptedException() {
        stubClient.withInterruptedException("Interrupted");

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("PETR3", result.getTicker());
        assertEquals("N/D", result.getPl());
    }

    @Test
    void fetchDataShouldReturnNDOnEmptyHtml() {
        stubClient.withResponse(200, "<html><body></body></html>");

        StockData result = fetcher.fetchData("PETR3");

        assertEquals("PETR3", result.getTicker());
        assertEquals("N/D", result.getPl());
        assertEquals("N/D", result.getDividendYield());
        assertEquals("N/D", result.getPVp());
        assertEquals("N/D", result.getRoe());
        assertEquals("N/D", result.getDividaLiquidaEbitda());
        assertEquals("N/D", result.getMargemEbitda());
        assertEquals("N/D", result.getLpa());
        assertEquals("N/D", result.getCrescimentoReceita5a());
    }

    // ========== fetchData - Mixed Content ==========

    @Test
    void fetchDataShouldExtractFromTextContainingElements() {
        String html = """
                <html><body>
                <div>
                  <span>P/L</span>
                  <strong>7,80</strong>
                </div>
                <div>
                  <span>DIVIDEND YIELD</span>
                  <strong>6,30%</strong>
                </div>
                <div>
                  <span>ROE</span>
                  <strong>18,50%</strong>
                </div>
                </body></html>
                """;

        stubClient.withResponse(200, html);

        StockData result = fetcher.fetchData("ITUB4");

        assertEquals("ITUB4", result.getTicker());
        assertEquals("7,80", result.getPl());
        assertEquals("6,30%", result.getDividendYield());
        assertEquals("18,50%", result.getRoe());
    }

    @Test
    void fetchDataShouldHandleNegativeValues() {
        String html = """
                <html><body>
                <div class="info">
                  <h3 class="title"><span>P/L</span></h3>
                  <div class="value"><strong class="value">-3,50</strong></div>
                </div>
                <div class="info">
                  <h3 class="title"><span>ROE</span></h3>
                  <div class="value"><strong class="value">-10,20%</strong></div>
                </div>
                </body></html>
                """;

        stubClient.withResponse(200, html);

        StockData result = fetcher.fetchData("OIBR3");

        assertEquals("-3,50", result.getPl());
        assertEquals("-10,20%", result.getRoe());
    }

    // ========== Constructor ==========

    @Test
    void defaultConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new StockDataFetcher());
    }

    @Test
    void sslDisabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new StockDataFetcher(true));
    }

    @Test
    void sslEnabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new StockDataFetcher(false));
    }
}
