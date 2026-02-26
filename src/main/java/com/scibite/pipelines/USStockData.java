package com.scibite.pipelines;

/**
 * Classe imutável que representa os indicadores financeiros de uma ação americana (US Stock).
 *
 * Indicadores de Valuation:
 * - P/E (Price/Earnings)
 * - P/S (Price/Sales)
 * - EV/EBITDA (Enterprise Value / EBITDA)
 * - P/BV (Price/Book Value)
 * - PEG (P/E ajustado por crescimento)
 * - FCF Yield (Free Cash Flow Yield)
 *
 * Métricas Financeiras:
 * - ROE (Return on Equity)
 * - Margem EBITDA
 * - Dívida/EBITDA (Debt/EBITDA)
 * - Current Ratio (Liquidez Corrente)
 * - Dividend Yield
 * - Payout Ratio
 */
public class USStockData {

    private final String ticker;
    private final String name;

    // Valuation
    private final String pe;
    private final String ps;
    private final String evEbitda;
    private final String pbv;
    private final String peg;
    private final String fcfYield;

    // Financial Metrics
    private final String roe;
    private final String margemEbitda;
    private final String debtEbitda;
    private final String currentRatio;
    private final String dividendYield;
    private final String payoutRatio;

    public USStockData(String ticker, String name,
                       String pe, String ps, String evEbitda, String pbv, String peg, String fcfYield,
                       String roe, String margemEbitda, String debtEbitda, String currentRatio,
                       String dividendYield, String payoutRatio) {
        this.ticker = ticker;
        this.name = name;
        this.pe = pe;
        this.ps = ps;
        this.evEbitda = evEbitda;
        this.pbv = pbv;
        this.peg = peg;
        this.fcfYield = fcfYield;
        this.roe = roe;
        this.margemEbitda = margemEbitda;
        this.debtEbitda = debtEbitda;
        this.currentRatio = currentRatio;
        this.dividendYield = dividendYield;
        this.payoutRatio = payoutRatio;
    }

    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getPe() { return pe; }
    public String getPs() { return ps; }
    public String getEvEbitda() { return evEbitda; }
    public String getPbv() { return pbv; }
    public String getPeg() { return peg; }
    public String getFcfYield() { return fcfYield; }
    public String getRoe() { return roe; }
    public String getMargemEbitda() { return margemEbitda; }
    public String getDebtEbitda() { return debtEbitda; }
    public String getCurrentRatio() { return currentRatio; }
    public String getDividendYield() { return dividendYield; }
    public String getPayoutRatio() { return payoutRatio; }

    @Override
    public String toString() {
        return "USStockData{ticker='" + ticker + "', name='" + name + "', P/E='" + pe
                + "', P/S='" + ps + "', EV/EBITDA='" + evEbitda + "', P/BV='" + pbv
                + "', PEG='" + peg + "', FCF Yield='" + fcfYield + "', ROE='" + roe
                + "', Marg.EBITDA='" + margemEbitda + "', Debt/EBITDA='" + debtEbitda
                + "', CurrentRatio='" + currentRatio + "', DY='" + dividendYield
                + "', Payout='" + payoutRatio + "'}";
    }
}
