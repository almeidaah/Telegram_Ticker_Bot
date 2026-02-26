package telegram.ticker.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe FIIData.
 */
class FIIDataTest {

    @Test
    void shouldStoreAllFieldsCorrectly() {
        FIIData data = new FIIData("HGLG11", "8,50%", "0,95", "245.320", "15");

        assertEquals("HGLG11", data.getTicker());
        assertEquals("8,50%", data.getDividendYield());
        assertEquals("0,95", data.getPVp());
        assertEquals("245.320", data.getCotistas());
        assertEquals("15", data.getImoveis());
    }

    @Test
    void shouldHandleNullValues() {
        FIIData data = new FIIData(null, null, null, null, null);

        assertNull(data.getTicker());
        assertNull(data.getDividendYield());
        assertNull(data.getPVp());
        assertNull(data.getCotistas());
        assertNull(data.getImoveis());
    }

    @Test
    void toStringShouldContainAllFields() {
        FIIData data = new FIIData("XPLG11", "9,20%", "1,05", "100.000", "20");
        String str = data.toString();

        assertTrue(str.contains("XPLG11"));
        assertTrue(str.contains("9,20%"));
        assertTrue(str.contains("1,05"));
        assertTrue(str.contains("100.000"));
        assertTrue(str.contains("20"));
    }
}
