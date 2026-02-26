package com.scibite.pipelines;

/**
 * Classe que representa os indicadores de uma criptomoeda.
 *
 * Contém preço atual, market cap e posição no ranking de criptomoedas.
 * É uma classe imutável para garantir a integridade dos dados.
 */
public class CryptoData {

    private final String symbol;
    private final String name;
    private final String price;
    private final String marketCap;
    private final String rank;

    /**
     * Construtor dos indicadores de criptomoeda.
     *
     * @param symbol    Símbolo da crypto (ex: BTC)
     * @param name      Nome completo (ex: Bitcoin)
     * @param price     Preço atual em USD (ex: "$42,150.00")
     * @param marketCap Market cap formatado (ex: "$820.5B")
     * @param rank      Posição no ranking (ex: "#1")
     */
    public CryptoData(String symbol, String name, String price, String marketCap, String rank) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.marketCap = marketCap;
        this.rank = rank;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getMarketCap() {
        return marketCap;
    }

    public String getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return String.format("CryptoData{symbol='%s', name='%s', price='%s', marketCap='%s', rank='%s'}",
                symbol, name, price, marketCap, rank);
    }
}
