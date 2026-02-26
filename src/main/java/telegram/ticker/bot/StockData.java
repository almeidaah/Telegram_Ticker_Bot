package telegram.ticker.bot;

/**
 * Classe imutável que representa os indicadores financeiros de uma ação brasileira.
 *
 * Indicadores:
 * - P/L (Preço/Lucro)
 * - DY (Dividend Yield)
 * - P/VP (Preço/Valor Patrimonial)
 * - ROE (Retorno sobre Patrimônio)
 * - Dívida Líquida/EBITDA
 * - Margem EBITDA
 * - LPA (Lucro por Ação)
 * - Crescimento de Receita 5 anos (CAGR)
 */
public class StockData {

    private final String ticker;
    private final String pl;
    private final String dividendYield;
    private final String pVp;
    private final String roe;
    private final String dividaLiquidaEbitda;
    private final String margemEbitda;
    private final String lpa;
    private final String crescimentoReceita5a;

    /**
     * Construtor completo.
     *
     * @param ticker               Código da ação (ex: PETR3)
     * @param pl                   P/L (Preço/Lucro)
     * @param dividendYield        Dividend Yield
     * @param pVp                  P/VP (Preço/Valor Patrimonial)
     * @param roe                  ROE (Retorno sobre Patrimônio)
     * @param dividaLiquidaEbitda  Dívida Líquida / EBITDA
     * @param margemEbitda         Margem EBITDA
     * @param lpa                  LPA (Lucro por Ação)
     * @param crescimentoReceita5a Crescimento de Receita 5 anos
     */
    public StockData(String ticker, String pl, String dividendYield, String pVp,
                     String roe, String dividaLiquidaEbitda, String margemEbitda,
                     String lpa, String crescimentoReceita5a) {
        this.ticker = ticker;
        this.pl = pl;
        this.dividendYield = dividendYield;
        this.pVp = pVp;
        this.roe = roe;
        this.dividaLiquidaEbitda = dividaLiquidaEbitda;
        this.margemEbitda = margemEbitda;
        this.lpa = lpa;
        this.crescimentoReceita5a = crescimentoReceita5a;
    }

    public String getTicker() { return ticker; }
    public String getPl() { return pl; }
    public String getDividendYield() { return dividendYield; }
    public String getPVp() { return pVp; }
    public String getRoe() { return roe; }
    public String getDividaLiquidaEbitda() { return dividaLiquidaEbitda; }
    public String getMargemEbitda() { return margemEbitda; }
    public String getLpa() { return lpa; }
    public String getCrescimentoReceita5a() { return crescimentoReceita5a; }

    @Override
    public String toString() {
        return "StockData{" +
                "ticker='" + ticker + '\'' +
                ", pl='" + pl + '\'' +
                ", dy='" + dividendYield + '\'' +
                ", pvp='" + pVp + '\'' +
                ", roe='" + roe + '\'' +
                ", divLiqEbitda='" + dividaLiquidaEbitda + '\'' +
                ", margemEbitda='" + margemEbitda + '\'' +
                ", lpa='" + lpa + '\'' +
                ", crescRec5a='" + crescimentoReceita5a + '\'' +
                '}';
    }
}
