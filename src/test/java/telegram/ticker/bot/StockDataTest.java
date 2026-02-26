package telegram.ticker.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe StockData.
 */
class StockDataTest {

    @Test
    void shouldStoreAllFieldsCorrectly() {
        StockData data = new StockData("PETR3", "5,20", "12,50%", "1,10",
                "25,30%", "1,80", "35,00%", "6,50", "8,00%");

        assertEquals("PETR3", data.getTicker());
        assertEquals("5,20", data.getPl());
        assertEquals("12,50%", data.getDividendYield());
        assertEquals("1,10", data.getPVp());
        assertEquals("25,30%", data.getRoe());
        assertEquals("1,80", data.getDividaLiquidaEbitda());
        assertEquals("35,00%", data.getMargemEbitda());
        assertEquals("6,50", data.getLpa());
        assertEquals("8,00%", data.getCrescimentoReceita5a());
    }

    @Test
    void shouldHandleNullValues() {
        StockData data = new StockData(null, null, null, null, null, null, null, null, null);

        assertNull(data.getTicker());
        assertNull(data.getPl());
        assertNull(data.getDividendYield());
        assertNull(data.getPVp());
        assertNull(data.getRoe());
        assertNull(data.getDividaLiquidaEbitda());
        assertNull(data.getMargemEbitda());
        assertNull(data.getLpa());
        assertNull(data.getCrescimentoReceita5a());
    }

    @Test
    void toStringShouldContainAllFields() {
        StockData data = new StockData("VALE3", "4,00", "10,00%", "1,50",
                "20,00%", "0,50", "40,00%", "8,00", "5,00%");
        String str = data.toString();

        assertTrue(str.contains("VALE3"));
        assertTrue(str.contains("4,00"));
        assertTrue(str.contains("10,00%"));
        assertTrue(str.contains("1,50"));
        assertTrue(str.contains("20,00%"));
    }
}
