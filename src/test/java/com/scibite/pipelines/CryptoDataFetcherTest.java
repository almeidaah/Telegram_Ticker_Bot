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
 * Testes unitários para CryptoDataFetcher.
 * Utiliza mocks do HttpClient para não depender de rede.
 */
@ExtendWith(MockitoExtension.class)
class CryptoDataFetcherTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private CryptoDataFetcher fetcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fetcher = new CryptoDataFetcher(httpClient, objectMapper);
    }

    // ========== isKnownCrypto ==========

    @Test
    void isKnownCryptoShouldReturnTrueForBTC() {
        assertTrue(CryptoDataFetcher.isKnownCrypto("BTC"));
    }

    @Test
    void isKnownCryptoShouldReturnTrueForLowercaseInput() {
        assertTrue(CryptoDataFetcher.isKnownCrypto("eth"));
    }

    @Test
    void isKnownCryptoShouldReturnFalseForUnknownSymbol() {
        assertFalse(CryptoDataFetcher.isKnownCrypto("XXXXXX"));
    }

    @Test
    void isKnownCryptoShouldRecognizeMajorCoins() {
        String[] knownCoins = {"BTC", "ETH", "SOL", "ADA", "DOGE", "XRP", "AVAX", "LINK", "DOT"};
        for (String coin : knownCoins) {
            assertTrue(CryptoDataFetcher.isKnownCrypto(coin), "Should recognize " + coin);
        }
    }

    // ========== fetchData - Success ==========

    @Test
    void fetchDataShouldReturnParsedCryptoDataOnSuccess() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 42150.50,
                    "market_cap": 820500000000,
                    "market_cap_rank": 1
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("Bitcoin", result.getName());
        assertEquals("$42,150.50", result.getPrice());
        assertEquals("$820.5B", result.getMarketCap());
        assertEquals("#1", result.getRank());
    }

    @Test
    void fetchDataShouldHandleLowPriceCrypto() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "shiba-inu",
                    "symbol": "shib",
                    "name": "Shiba Inu",
                    "current_price": 0.00001234,
                    "market_cap": 7200000000,
                    "market_cap_rank": 15
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("SHIB");

        assertEquals("SHIB", result.getSymbol());
        assertEquals("Shiba Inu", result.getName());
        assertTrue(result.getPrice().startsWith("$0.0000"));
        assertEquals("$7.2B", result.getMarketCap());
        assertEquals("#15", result.getRank());
    }

    @Test
    void fetchDataShouldHandleTrillionMarketCap() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 65000.00,
                    "market_cap": 1300000000000,
                    "market_cap_rank": 1
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("$1.3T", result.getMarketCap());
    }

    @Test
    void fetchDataShouldHandleMillionMarketCap() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "some-coin",
                    "symbol": "xyz",
                    "name": "SomeCoin",
                    "current_price": 1.50,
                    "market_cap": 500000000,
                    "market_cap_rank": 200
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("SOL");

        assertEquals("$500.0M", result.getMarketCap());
    }

    @Test
    void fetchDataShouldConvertSymbolToUpperCase() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "ethereum",
                    "symbol": "eth",
                    "name": "Ethereum",
                    "current_price": 2300.00,
                    "market_cap": 276000000000,
                    "market_cap_rank": 2
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("eth");

        assertEquals("ETH", result.getSymbol());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNDOnHttpError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldReturnNDOnEmptyJsonArray() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("[]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldReturnNDOnException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Connection refused"));

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("N/D", result.getPrice());
    }

    @Test
    void fetchDataShouldHandleNullMarketCapRank() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 42000.00,
                    "market_cap": 800000000000,
                    "market_cap_rank": null
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldHandleZeroPriceAndMarketCap() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "current_price": 0,
                    "market_cap": 0,
                    "market_cap_rank": 1
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
    }

    @Test
    void fetchDataShouldHandleMissingFields() throws Exception {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc"
                  }
                ]
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        // name defaults to symbol when missing
        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
        assertEquals("N/D", result.getRank());
    }

    // ========== getCryptoName ==========

    @Test
    void getCryptoNameShouldReturnCoinGeckoIdForKnownCrypto() {
        String name = fetcher.getCryptoName("BTC");
        assertEquals("bitcoin", name);
    }

    @Test
    void getCryptoNameShouldReturnSymbolForUnknownCrypto() {
        String name = fetcher.getCryptoName("XXXXXX");
        assertEquals("XXXXXX", name);
    }
}
