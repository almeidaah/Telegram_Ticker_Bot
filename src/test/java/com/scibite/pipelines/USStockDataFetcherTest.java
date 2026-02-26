package com.scibite.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Testes unitários para USStockDataFetcher (ações americanas).
 * Utiliza mocks do HttpClient para não depender de rede.
 */
@ExtendWith(MockitoExtension.class)
class USStockDataFetcherTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private USStockDataFetcher fetcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fetcher = new USStockDataFetcher(httpClient, objectMapper);
    }

    // ========== fetchData - Success ==========

    @Test
    void fetchDataShouldReturnParsedUSStockDataOnSuccess() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {
                          "enterpriseToEbitda": {"raw": 22.10, "fmt": "22.10"},
                          "priceToBook": {"raw": 45.50, "fmt": "45.50"},
                          "pegRatio": {"raw": 2.80, "fmt": "2.80"}
                        },
                        "financialData": {
                          "returnOnEquity": {"raw": 1.50, "fmt": "150.00%"},
                          "ebitdaMargins": {"raw": 0.335, "fmt": "33.50%"},
                          "currentRatio": {"raw": 1.05, "fmt": "1.05"},
                          "freeCashflow": {"raw": 100000000000, "fmt": "100B"},
                          "totalDebt": {"raw": 120000000000, "fmt": "120B"},
                          "ebitda": {"raw": 130000000000, "fmt": "130B"}
                        },
                        "summaryDetail": {
                          "trailingPE": {"raw": 28.45, "fmt": "28.45"},
                          "priceToSalesTrailing12Months": {"raw": 7.20, "fmt": "7.20"},
                          "dividendYield": {"raw": 0.0055, "fmt": "0.55%"},
                          "payoutRatio": {"raw": 0.155, "fmt": "15.50%"},
                          "marketCap": {"raw": 3000000000000, "fmt": "3T"}
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("AAPL", result.getTicker());
        assertEquals("28.45", result.getPe());
        assertEquals("7.20", result.getPs());
        assertEquals("22.10", result.getEvEbitda());
        assertEquals("45.50", result.getPbv());
        assertEquals("2.80", result.getPeg());
        assertNotEquals("N/A", result.getFcfYield());
        assertEquals("150.00%", result.getRoe());
        assertEquals("33.50%", result.getMargemEbitda());
        assertNotEquals("N/A", result.getDebtEbitda());
        assertEquals("1.05", result.getCurrentRatio());
        assertEquals("0.55%", result.getDividendYield());
        assertEquals("15.50%", result.getPayoutRatio());
    }

    @Test
    void fetchDataShouldConvertTickerToUpperCase() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {},
                        "summaryDetail": {}
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("aapl");

        assertEquals("AAPL", result.getTicker());
    }

    @Test
    void fetchDataShouldUseFmtValueWhenAvailable() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {
                          "enterpriseToEbitda": {"raw": 22.1234, "fmt": "22.12"}
                        },
                        "financialData": {},
                        "summaryDetail": {
                          "trailingPE": {"raw": 28.4567, "fmt": "28.46"}
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        // Should use fmt (formatted) value
        assertEquals("28.46", result.getPe());
        assertEquals("22.12", result.getEvEbitda());
    }

    @Test
    void fetchDataShouldFallbackToRawWhenFmtIsAbsent() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {
                          "priceToBook": {"raw": 12.34}
                        },
                        "financialData": {},
                        "summaryDetail": {}
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("12.34x", result.getPbv());
    }

    @Test
    void fetchDataShouldCalculateFcfYield() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {
                          "freeCashflow": {"raw": 50000000000}
                        },
                        "summaryDetail": {
                          "marketCap": {"raw": 1000000000000}
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        // FCF Yield = 50B / 1T = 5%
        assertEquals("5.00%", result.getFcfYield());
    }

    @Test
    void fetchDataShouldCalculateDebtEbitda() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {
                          "totalDebt": {"raw": 100000000000},
                          "ebitda": {"raw": 50000000000}
                        },
                        "summaryDetail": {}
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        // Debt/EBITDA = 100B / 50B = 2.0
        assertEquals("2.00x", result.getDebtEbitda());
    }

    @Test
    void fetchDataShouldFormatPercentValues() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {
                          "returnOnEquity": {"raw": 0.2345, "fmt": "23.45%"},
                          "ebitdaMargins": {"raw": 0.40, "fmt": "40.00%"}
                        },
                        "summaryDetail": {
                          "dividendYield": {"raw": 0.015, "fmt": "1.50%"},
                          "payoutRatio": {"raw": 0.25, "fmt": "25.00%"}
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("MSFT");

        assertEquals("23.45%", result.getRoe());
        assertEquals("40.00%", result.getMargemEbitda());
        assertEquals("1.50%", result.getDividendYield());
        assertEquals("25.00%", result.getPayoutRatio());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNAOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("INVALID");

        assertEquals("INVALID", result.getTicker());
        assertEquals("N/A", result.getPe());
        assertEquals("N/A", result.getPs());
        assertEquals("N/A", result.getEvEbitda());
        assertEquals("N/A", result.getPbv());
        assertEquals("N/A", result.getPeg());
        assertEquals("N/A", result.getFcfYield());
        assertEquals("N/A", result.getRoe());
        assertEquals("N/A", result.getMargemEbitda());
        assertEquals("N/A", result.getDebtEbitda());
        assertEquals("N/A", result.getCurrentRatio());
        assertEquals("N/A", result.getDividendYield());
        assertEquals("N/A", result.getPayoutRatio());
    }

    @Test
    void fetchDataShouldReturnNAOnServerError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("N/A", result.getPe());
    }

    @Test
    void fetchDataShouldReturnNAOnEmptyResult() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": []
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("AAPL", result.getTicker());
        assertEquals("N/A", result.getPe());
    }

    @Test
    void fetchDataShouldReturnNAOnIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Connection reset"));

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("AAPL", result.getTicker());
        assertEquals("N/A", result.getPe());
        assertEquals("N/A", result.getRoe());
    }

    @Test
    void fetchDataShouldReturnNAOnInterruptedException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("AAPL", result.getTicker());
        assertEquals("N/A", result.getPe());
    }

    @Test
    void fetchDataShouldReturnNAForMissingFields() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {},
                        "summaryDetail": {}
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("AAPL", result.getTicker());
        assertEquals("N/A", result.getPe());
        assertEquals("N/A", result.getPs());
        assertEquals("N/A", result.getEvEbitda());
        assertEquals("N/A", result.getPbv());
        assertEquals("N/A", result.getPeg());
        assertEquals("N/A", result.getFcfYield());
        assertEquals("N/A", result.getRoe());
        assertEquals("N/A", result.getMargemEbitda());
        assertEquals("N/A", result.getDebtEbitda());
        assertEquals("N/A", result.getCurrentRatio());
        assertEquals("N/A", result.getDividendYield());
        assertEquals("N/A", result.getPayoutRatio());
    }

    @Test
    void fetchDataShouldHandleNullFieldValues() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {
                          "priceToBook": null,
                          "pegRatio": null
                        },
                        "financialData": {
                          "returnOnEquity": null
                        },
                        "summaryDetail": {
                          "trailingPE": null,
                          "dividendYield": null
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("N/A", result.getPe());
        assertEquals("N/A", result.getPbv());
        assertEquals("N/A", result.getPeg());
        assertEquals("N/A", result.getRoe());
        assertEquals("N/A", result.getDividendYield());
    }

    @Test
    void fetchDataShouldReturnNAWhenFcfOrMarketCapIsZero() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {
                          "freeCashflow": {"raw": 0}
                        },
                        "summaryDetail": {
                          "marketCap": {"raw": 0}
                        }
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("N/A", result.getFcfYield());
    }

    @Test
    void fetchDataShouldReturnNAWhenDebtOrEbitdaIsZero() throws Exception {
        String jsonResponse = """
                {
                  "quoteSummary": {
                    "result": [
                      {
                        "defaultKeyStatistics": {},
                        "financialData": {
                          "totalDebt": {"raw": 0},
                          "ebitda": {"raw": 0}
                        },
                        "summaryDetail": {}
                      }
                    ]
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        USStockData result = fetcher.fetchData("AAPL");

        assertEquals("N/A", result.getDebtEbitda());
    }

    // ========== Constructor ==========

    @Test
    void defaultConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new USStockDataFetcher());
    }

    @Test
    void sslDisabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new USStockDataFetcher(true));
    }

    @Test
    void sslEnabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new USStockDataFetcher(false));
    }
}
