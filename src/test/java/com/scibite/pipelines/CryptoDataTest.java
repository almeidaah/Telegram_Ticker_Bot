package com.scibite.pipelines;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe CryptoData.
 */
class CryptoDataTest {

    @Test
    void shouldStoreAllFieldsCorrectly() {
        CryptoData data = new CryptoData("BTC", "Bitcoin", "$42,150.00", "$820.5B", "#1");

        assertEquals("BTC", data.getSymbol());
        assertEquals("Bitcoin", data.getName());
        assertEquals("$42,150.00", data.getPrice());
        assertEquals("$820.5B", data.getMarketCap());
        assertEquals("#1", data.getRank());
    }

    @Test
    void shouldHandleNullValues() {
        CryptoData data = new CryptoData(null, null, null, null, null);

        assertNull(data.getSymbol());
        assertNull(data.getName());
        assertNull(data.getPrice());
        assertNull(data.getMarketCap());
        assertNull(data.getRank());
    }

    @Test
    void toStringShouldContainAllFields() {
        CryptoData data = new CryptoData("ETH", "Ethereum", "$2,300.50", "$276.0B", "#2");
        String str = data.toString();

        assertTrue(str.contains("ETH"));
        assertTrue(str.contains("Ethereum"));
        assertTrue(str.contains("$2,300.50"));
        assertTrue(str.contains("$276.0B"));
        assertTrue(str.contains("#2"));
    }
}
