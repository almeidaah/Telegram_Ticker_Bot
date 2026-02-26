package com.scibite.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CryptoDataFetcher.
 * Utiliza StubHttpClient para não depender de rede.
 */
class CryptoDataFetcherTest {

    private StubHttpClient stubClient;
    private CryptoDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        stubClient = new StubHttpClient();
        fetcher = new CryptoDataFetcher(stubClient, new ObjectMapper());
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
    void fetchDataShouldReturnParsedCryptoDataOnSuccess() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("Bitcoin", result.getName());
        assertEquals("$42,150.50", result.getPrice());
        assertEquals("$820.5B", result.getMarketCap());
        assertEquals("#1", result.getRank());
    }

    @Test
    void fetchDataShouldHandleLowPriceCrypto() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("SHIB");

        assertEquals("SHIB", result.getSymbol());
        assertEquals("Shiba Inu", result.getName());
        assertTrue(result.getPrice().startsWith("$0.0000"));
        assertEquals("$7.2B", result.getMarketCap());
        assertEquals("#15", result.getRank());
    }

    @Test
    void fetchDataShouldHandleTrillionMarketCap() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("$1.3T", result.getMarketCap());
    }

    @Test
    void fetchDataShouldHandleMillionMarketCap() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("SOL");

        assertEquals("$500.0M", result.getMarketCap());
    }

    @Test
    void fetchDataShouldConvertSymbolToUpperCase() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("eth");

        assertEquals("ETH", result.getSymbol());
    }

    // ========== fetchData - Error Cases ==========

    @Test
    void fetchDataShouldReturnNDOnHttpError() {
        stubClient.withResponse(500, "");

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldReturnNDOnEmptyJsonArray() {
        stubClient.withResponse(200, "[]");

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldReturnNDOnIOException() {
        stubClient.withIOException("Connection refused");

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("N/D", result.getPrice());
    }

    @Test
    void fetchDataShouldReturnNDOnInterruptedException() {
        stubClient.withInterruptedException("Interrupted");

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
        assertEquals("N/D", result.getPrice());
    }

    @Test
    void fetchDataShouldHandleNullMarketCapRank() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getRank());
    }

    @Test
    void fetchDataShouldHandleZeroPriceAndMarketCap() {
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

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("N/D", result.getPrice());
        assertEquals("N/D", result.getMarketCap());
    }

    @Test
    void fetchDataShouldHandleMissingFields() {
        String jsonResponse = """
                [
                  {
                    "id": "bitcoin",
                    "symbol": "btc"
                  }
                ]
                """;

        stubClient.withResponse(200, jsonResponse);

        CryptoData result = fetcher.fetchData("BTC");

        assertEquals("BTC", result.getSymbol());
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

    // ========== Constructor ==========

    @Test
    void defaultConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new CryptoDataFetcher());
    }

    @Test
    void sslDisabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new CryptoDataFetcher(true));
    }

    @Test
    void sslEnabledConstructorShouldNotThrow() {
        assertDoesNotThrow(() -> new CryptoDataFetcher(false));
    }
}
