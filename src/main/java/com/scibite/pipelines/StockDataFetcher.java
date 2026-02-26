package com.scibite.pipelines;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Classe utilitária para buscar indicadores financeiros de ações brasileiras.
 *
 * Faz scraping da página da ação no StatusInvest para obter:
 * - P/L (Preço/Lucro)
 * - DY (Dividend Yield)
 * - P/VP (Preço/Valor Patrimonial)
 * - ROE (Retorno sobre Patrimônio)
 * - Dívida Líquida/EBITDA
 * - Margem EBITDA
 * - LPA (Lucro por Ação)
 * - Crescimento de Receita 5 anos (CAGR)
 *
 * URL base: https://statusinvest.com.br/acoes/{ticker}
 */
public class StockDataFetcher {

    private static final String STATUS_INVEST_URL = "https://statusinvest.com.br/acoes/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public StockDataFetcher() {
        this(false);
    }

    /**
     * Construtor com opção de SSL permissivo.
     *
     * @param disableSslVerification Se true, desabilita verificação de certificados SSL
     */
    public StockDataFetcher(boolean disableSslVerification) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL permissivo no StockDataFetcher: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
    }

    // Package-private constructor for testing
    StockDataFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Busca indicadores financeiros de uma ação brasileira.
     *
     * @param ticker Código da ação (ex: PETR3, VALE3, ITUB4)
     * @return StockData com os indicadores encontrados (ou "N/D" para indisponíveis)
     */
    public StockData fetchData(String ticker) {
        String pl = "N/D";
        String dy = "N/D";
        String pvp = "N/D";
        String roe = "N/D";
        String divLiqEbitda = "N/D";
        String margemEbitda = "N/D";
        String lpa = "N/D";
        String crescRec5a = "N/D";

        try {
            String url = STATUS_INVEST_URL + ticker.toLowerCase();
            System.out.println("📈 Buscando indicadores da ação: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠️ StockDataFetcher - Status HTTP: " + response.statusCode() + " para " + ticker);
                return new StockData(ticker, pl, dy, pvp, roe, divLiqEbitda, margemEbitda, lpa, crescRec5a);
            }

            Document doc = Jsoup.parse(response.body());

            // StatusInvest usa a estrutura:
            // <div class="info">
            //   <h3 class="title"><span>INDICADOR</span></h3>
            //   <div class="value"><strong>VALOR</strong></div>
            // </div>
            // Também usa atributos data-* em elementos com class "value"

            pl = extractStatusInvestValue(doc, "P/L");
            dy = extractStatusInvestValue(doc, "DY", "DIV. YIELD", "DIVIDEND YIELD", "Div. Yield");
            pvp = extractStatusInvestValue(doc, "P/VP", "P/VPA");
            roe = extractStatusInvestValue(doc, "ROE");
            divLiqEbitda = extractStatusInvestValue(doc, "D\u00cdV. L\u00cdQUIDA / EBITDA",
                    "DIV. LIQUIDA / EBITDA", "D. L\u00cdQ/EBITDA", "DÍV. LÍQ. / EBITDA",
                    "DÍVIDA LÍQ/EBITDA", "LIQ. CORRENTE");
            margemEbitda = extractStatusInvestValue(doc, "MARG. EBITDA", "MARGEM EBITDA", "M. EBITDA");
            lpa = extractStatusInvestValue(doc, "LPA");
            crescRec5a = extractStatusInvestValue(doc, "CAGR RECEITAS 5 ANOS", "C.A.G.R. RECEITAS 5 ANOS",
                    "CRESC. REC. 5A", "CAGR RECEITA 5 ANOS", "CAGR REC 5A");

            System.out.println("📊 " + ticker + " → P/L: " + pl + " | DY: " + dy + " | P/VP: " + pvp
                    + " | ROE: " + roe + " | Div/EBITDA: " + divLiqEbitda + " | Marg.EBITDA: " + margemEbitda
                    + " | LPA: " + lpa + " | CrescRec5a: " + crescRec5a);

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro ao buscar indicadores de " + ticker + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar indicadores de " + ticker + ": " + e.getMessage());
        }

        return new StockData(ticker, pl, dy, pvp, roe, divLiqEbitda, margemEbitda, lpa, crescRec5a);
    }

    /**
     * Extrai o valor de um indicador do StatusInvest.
     *
     * O StatusInvest usa a seguinte estrutura de HTML:
     * <div class="info">
     *   <h3 class="title"><span>NOME_INDICADOR</span></h3>
     *   <div class="value">
     *     <strong class="value" ...>VALOR</strong>
     *   </div>
     * </div>
     *
     * Estratégias de busca:
     * 1. Busca por texto exato do indicador em títulos (h3.title span)
     * 2. Busca por texto contendo o nome nos elementos da página
     * 3. Procura o valor numérico próximo ao label encontrado
     *
     * @param doc   Documento Jsoup parseado
     * @param names Nomes possíveis do indicador
     * @return Valor encontrado ou "N/D"
     */
    private String extractStatusInvestValue(Document doc, String... names) {
        for (String name : names) {
            // Estratégia 1: Busca h3.title contendo o texto do indicador
            Elements titleElements = doc.select("h3.title span, h3 span.title, span.title");
            for (Element titleEl : titleElements) {
                String titleText = titleEl.text().trim().toUpperCase();
                if (titleText.contains(name.toUpperCase())) {
                    // Encontrou o título, agora busca o valor
                    String value = findValueInParentContainer(titleEl);
                    if (value != null) return value;
                }
            }

            // Estratégia 2: Busca por elementos que contêm o texto do indicador
            Elements matchingElements = doc.getElementsContainingOwnText(name);
            for (Element el : matchingElements) {
                // Ignora se é um elemento muito grande (como body, div principal)
                if (el.text().length() > 200) continue;

                String value = findValueNear(el);
                if (value != null) return value;
            }

            // Estratégia 3: Busca em atributos title ou data-*
            Elements withTitle = doc.select("[title*=" + cssEscape(name) + "]");
            for (Element el : withTitle) {
                String value = findValueNear(el);
                if (value != null) return value;
            }
        }

        return "N/D";
    }

    /**
     * Busca o valor dentro do container pai de um elemento title.
     * No StatusInvest, o valor geralmente está em um <strong> dentro de um <div class="value">
     * que é irmão ou está dentro do mesmo .info container que o título.
     */
    private String findValueInParentContainer(Element titleElement) {
        // Sobe até encontrar um container .info ou similar
        Element container = titleElement;
        for (int i = 0; i < 5; i++) {
            container = container.parent();
            if (container == null) break;

            // Verifica se estamos no container .info
            String className = container.className();
            if (className.contains("info") || className.contains("card") || className.contains("item")) {
                // Busca strong.value ou div.value strong dentro deste container
                Elements strongValues = container.select("strong.value, strong[class*=value], div.value strong");
                for (Element sv : strongValues) {
                    String val = extractNumericText(sv);
                    if (val != null) return val;
                }

                // Busca qualquer strong com valor numérico
                Elements allStrongs = container.select("strong");
                for (Element s : allStrongs) {
                    String val = extractNumericText(s);
                    if (val != null) return val;
                }

                // Busca divs com classe value
                Elements divValues = container.select("div.value, span.value");
                for (Element dv : divValues) {
                    String val = extractNumericText(dv);
                    if (val != null) return val;
                }
            }
        }

        return null;
    }

    /**
     * Procura um valor numérico perto de um elemento no DOM.
     */
    private String findValueNear(Element labelElement) {
        // Verifica o pai e seus filhos
        Element parent = labelElement.parent();
        if (parent != null) {
            for (Element sibling : parent.children()) {
                if (sibling == labelElement) continue;
                String val = extractNumericText(sibling);
                if (val != null) return val;
            }

            Element grandparent = parent.parent();
            if (grandparent != null) {
                for (Element child : grandparent.children()) {
                    if (child == parent) continue;
                    String val = extractNumericText(child);
                    if (val != null) return val;
                }

                Elements valueElements = grandparent.select("strong, b, span.value, div.value");
                for (Element ve : valueElements) {
                    String val = extractNumericText(ve);
                    if (val != null) return val;
                }
            }
        }

        Element next = labelElement.nextElementSibling();
        if (next != null) {
            String val = extractNumericText(next);
            if (val != null) return val;
        }

        return null;
    }

    /**
     * Extrai texto numérico de um elemento.
     * Aceita formatos como: "8,50%", "0,95", "-3,20", "R$ 160,50", "1.234,56"
     */
    private String extractNumericText(Element element) {
        String text = element.text().trim();

        if (text.isEmpty() || !text.matches(".*\\d.*")) {
            return null;
        }

        // Remove prefixos como "R$"
        text = text.replaceAll("^R\\$\\s*", "").trim();

        // Verifica formato numérico: aceita dígitos, vírgula, ponto, %, sinal negativo
        if (text.matches("^-?[\\d.,]+%?$")) {
            return text;
        }

        // Tenta extrair de dentro do texto
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?[\\d.,]+%?").matcher(text);
        if (matcher.find()) {
            String found = matcher.group();
            // Elimina valores muito curtos que podem ser ruído
            if (found.replace(",", "").replace(".", "").replace("%", "").replace("-", "").length() >= 1) {
                return found;
            }
        }

        return null;
    }

    /**
     * Escapa caracteres especiais para uso em seletores CSS.
     */
    private String cssEscape(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "\\\\$0");
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
