package telegram.ticker.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe USStockData.
 */
class USStockDataTest {

    @Test
    void shouldStoreAllFieldsCorrectly() {
        USStockData data = new USStockData("AAPL", "Apple Inc.",
                "28.45x", "7.20x", "22.10x", "45.50x", "2.80x", "3.50%",
                "150.00%", "33.50%", "1.20x", "1.05x", "0.55%", "15.50%");

        assertEquals("AAPL", data.getTicker());
        assertEquals("Apple Inc.", data.getName());
        assertEquals("28.45x", data.getPe());
        assertEquals("7.20x", data.getPs());
        assertEquals("22.10x", data.getEvEbitda());
        assertEquals("45.50x", data.getPbv());
        assertEquals("2.80x", data.getPeg());
        assertEquals("3.50%", data.getFcfYield());
        assertEquals("150.00%", data.getRoe());
        assertEquals("33.50%", data.getMargemEbitda());
        assertEquals("1.20x", data.getDebtEbitda());
        assertEquals("1.05x", data.getCurrentRatio());
        assertEquals("0.55%", data.getDividendYield());
        assertEquals("15.50%", data.getPayoutRatio());
    }

    @Test
    void shouldHandleNullValues() {
        USStockData data = new USStockData(null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertNull(data.getTicker());
        assertNull(data.getName());
        assertNull(data.getPe());
        assertNull(data.getPs());
    }

    @Test
    void toStringShouldContainAllFields() {
        USStockData data = new USStockData("MSFT", "Microsoft Corp.",
                "30.00x", "12.00x", "25.00x", "14.00x", "2.50x", "2.80%",
                "40.00%", "50.00%", "0.80x", "1.80x", "0.75%", "25.00%");
        String str = data.toString();

        assertTrue(str.contains("MSFT"));
        assertTrue(str.contains("Microsoft Corp."));
        assertTrue(str.contains("30.00x"));
    }
}
