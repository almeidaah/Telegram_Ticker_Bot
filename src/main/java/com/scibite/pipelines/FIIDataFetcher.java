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
 * Classe utilitária para buscar indicadores financeiros de FIIs.
 *
 * Faz scraping da página do FII no Funds Explorer para obter:
 * - Dividend Yield (DY)
 * - P/VP (Preço sobre Valor Patrimonial)
 * - Número de cotistas
 * - Número de imóveis
 *
 * URL base: https://www.fundsexplorer.com.br/funds/{ticker}
 */
public class FIIDataFetcher {

    private static final String FUNDS_EXPLORER_URL = "https://www.fundsexplorer.com.br/funds/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public FIIDataFetcher() {
        this(false);
    }

    /**
     * Construtor com opção de SSL permissivo.
     *
     * @param disableSslVerification Se true, desabilita verificação de certificados SSL
     */
    public FIIDataFetcher(boolean disableSslVerification) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL permissivo no FIIDataFetcher: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
    }

    // Package-private constructor for testing
    FIIDataFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Busca indicadores financeiros de um ticker de FII.
     *
     * @param ticker Código do fundo imobiliário (ex: HGLG11)
     * @return FIIData com os indicadores encontrados (ou "N/D" para indisponíveis)
     */
    public FIIData fetchData(String ticker) {
        String dy = "N/D";
        String pvp = "N/D";
        String cotistas = "N/D";
        String imoveis = "N/D";

        try {
            String url = FUNDS_EXPLORER_URL + ticker.toUpperCase();
            System.out.println("📈 Buscando indicadores: " + url);

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
                System.err.println("⚠️ FIIDataFetcher - Status HTTP: " + response.statusCode() + " para " + ticker);
                return new FIIData(ticker, dy, pvp, cotistas, imoveis);
            }

            Document doc = Jsoup.parse(response.body());

            // Tenta extrair de Funds Explorer
            dy = extractIndicator(doc, "Dividend Yield", "DY", "Div. Yield");
            pvp = extractIndicator(doc, "P/VP", "P/VPA", "Preço/VP");
            cotistas = extractLargeNumber(doc, "Cotistas", "Número de Cotistas", "Nº Cotistas", "N. Cotistas");
            imoveis = extractLargeNumber(doc, "Imóveis", "Imoveis", "Ativos", "Número de Imóveis", "Qtd. Imóveis", "Qtd Ativos");

            System.out.println("📊 " + ticker + " → DY: " + dy + " | P/VP: " + pvp + " | Cotistas: " + cotistas + " | Imóveis: " + imoveis);

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro ao buscar indicadores de " + ticker + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar indicadores de " + ticker + ": " + e.getMessage());
        }

        return new FIIData(ticker, dy, pvp, cotistas, imoveis);
    }

    /**
     * Extrai o valor de um indicador buscando por múltiplos nomes possíveis no HTML.
     * Usa várias estratégias de busca no DOM para ser robusto contra mudanças no layout.
     *
     * @param doc       Documento Jsoup já parseado
     * @param names     Nomes possíveis do indicador (ex: "Dividend Yield", "DY")
     * @return Valor encontrado ou "N/D" se não encontrado
     */
    private String extractIndicator(Document doc, String... names) {
        for (String name : names) {
            // Estratégia 1: Elementos que contêm exatamente o texto do indicador
            Elements elements = doc.getElementsContainingOwnText(name);
            for (Element el : elements) {
                String value = findValueNear(el);
                if (value != null) {
                    return value;
                }
            }

            // Estratégia 2: Busca por spans/divs com class contendo o nome
            String safeName = name.toLowerCase().replace("/", "").replace(" ", "-");
            Elements byClass = doc.select("[class*=" + safeName + "]");
            for (Element el : byClass) {
                String value = extractNumericValue(el);
                if (value != null) {
                    return value;
                }
            }
        }

        return "N/D";
    }

    /**
     * Procura um valor numérico perto de um elemento label no DOM.
     * Verifica irmãos, pais e filhos do elemento.
     */
    private String findValueNear(Element labelElement) {
        // Verifica o pai imediato e seus filhos
        Element parent = labelElement.parent();
        if (parent != null) {
            // Busca em irmãos do label
            for (Element sibling : parent.children()) {
                if (sibling == labelElement) continue;
                String val = extractNumericValue(sibling);
                if (val != null) return val;
            }

            // Sobe mais um nível e busca nos filhos
            Element grandparent = parent.parent();
            if (grandparent != null) {
                for (Element child : grandparent.children()) {
                    if (child == parent) continue;
                    String val = extractNumericValue(child);
                    if (val != null) return val;
                }
                // Busca strong/b/span com valor numérico no container
                Elements valueElements = grandparent.select("strong, b, span.value, div.value, p.value");
                for (Element ve : valueElements) {
                    String val = extractNumericValue(ve);
                    if (val != null) return val;
                }
            }
        }

        // Busca no próximo irmão do label
        Element next = labelElement.nextElementSibling();
        if (next != null) {
            String val = extractNumericValue(next);
            if (val != null) return val;
        }

        return null;
    }

    /**
     * Extrai um valor numérico (com %, vírgula, ponto) de um elemento.
     * Aceita formatos como: "8,50%", "0,95", "3.20%", "R$ 160,50"
     */
    private String extractNumericValue(Element element) {
        String text = element.text().trim();

        // Remove espaços extras e verifica se contém algum dígito
        if (text.isEmpty() || !text.matches(".*\\d.*")) {
            return null;
        }

        // Ignora textos longos (provavelmente não são valores numéricos)
        if (text.length() > 30) {
            return null;
        }

        // Limpa prefixos comuns
        text = text.replaceAll("^R\\$\\s*", "").trim();

        // Verifica se parece um valor numérico (com separadores brasileiros)
        if (text.matches("-?\\d{1,3}([.,]\\d{1,4})?%?")) {
            return text;
        }

        return null;
    }

    /**
     * Extrai um número grande (cotistas, imóveis) buscando por múltiplos nomes no HTML.
     * Aceita formatos como: "245.320", "15", "1.234.567"
     */
    private String extractLargeNumber(Document doc, String... names) {
        for (String name : names) {
            Elements elements = doc.getElementsContainingOwnText(name);
            for (Element el : elements) {
                String value = findLargeNumberNear(el);
                if (value != null) {
                    return value;
                }
            }
        }
        return "N/D";
    }

    /**
     * Procura um número grande perto de um elemento label no DOM.
     */
    private String findLargeNumberNear(Element labelElement) {
        Element parent = labelElement.parent();
        if (parent != null) {
            for (Element sibling : parent.children()) {
                if (sibling == labelElement) continue;
                String val = extractLargeNumericValue(sibling);
                if (val != null) return val;
            }

            Element grandparent = parent.parent();
            if (grandparent != null) {
                for (Element child : grandparent.children()) {
                    if (child == parent) continue;
                    String val = extractLargeNumericValue(child);
                    if (val != null) return val;
                }
                Elements valueElements = grandparent.select("strong, b, span.value, div.value, p.value");
                for (Element ve : valueElements) {
                    String val = extractLargeNumericValue(ve);
                    if (val != null) return val;
                }
            }
        }

        Element next = labelElement.nextElementSibling();
        if (next != null) {
            String val = extractLargeNumericValue(next);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * Extrai um valor numérico grande (ex: "245.320", "15", "1.234.567").
     */
    private String extractLargeNumericValue(Element element) {
        String text = element.text().trim();
        if (text.isEmpty() || !text.matches(".*\\d.*") || text.length() > 30) {
            return null;
        }
        // Aceita números com separadores de milhares (ponto brasileiro ou vírgula)
        if (text.matches("\\d{1,3}(\\.\\d{3})*") || text.matches("\\d+")) {
            return text;
        }
        return null;
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
