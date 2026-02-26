package com.scibite.pipelines;

/**
 * Classe que representa os indicadores financeiros de um FII.
 *
 * Contém Dividend Yield, P/VP, número de cotistas e número de imóveis.
 * É uma classe imutável para garantir a integridade dos dados.
 */
public class FIIData {

    private final String ticker;
    private final String dividendYield;
    private final String pVp;
    private final String cotistas;
    private final String imoveis;

    /**
     * Construtor dos indicadores do FII.
     *
     * @param ticker        Código do fundo (ex: HGLG11)
     * @param dividendYield Dividend Yield do fundo (ex: "8,50%")
     * @param pVp           P/VP do fundo (ex: "0,95")
     * @param cotistas      Número de cotistas (ex: "245.320")
     * @param imoveis       Número de imóveis (ex: "15")
     */
    public FIIData(String ticker, String dividendYield, String pVp, String cotistas, String imoveis) {
        this.ticker = ticker;
        this.dividendYield = dividendYield;
        this.pVp = pVp;
        this.cotistas = cotistas;
        this.imoveis = imoveis;
    }

    public String getTicker() {
        return ticker;
    }

    public String getDividendYield() {
        return dividendYield;
    }

    public String getPVp() {
        return pVp;
    }

    public String getCotistas() {
        return cotistas;
    }

    public String getImoveis() {
        return imoveis;
    }

    @Override
    public String toString() {
        return String.format("FIIData{ticker='%s', dy='%s', pvp='%s', cotistas='%s', imoveis='%s'}",
                ticker, dividendYield, pVp, cotistas, imoveis);
    }
}
