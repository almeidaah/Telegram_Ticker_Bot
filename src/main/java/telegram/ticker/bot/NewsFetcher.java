package telegram.ticker.bot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Classe utilitária para buscar notícias de FIIs usando Google News RSS.
 *
 * O Google News fornece um feed RSS público que pode ser acessado
 * passando uma query de busca na URL. O feed retorna um XML com
 * as notícias mais recentes relacionadas à busca.
 *
 * URL base: https://news.google.com/rss/search?q={query}&hl=pt-BR&gl=BR&ceid=BR:pt-419
 */
public class NewsFetcher {

    // URL base do Google News RSS
    private static final String GOOGLE_NEWS_RSS_URL = "https://news.google.com/rss/search";

    // Cliente HTTP reutilizável (thread-safe)
    private final HttpClient httpClient;

    // Timeout para requisições HTTP
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Construtor que inicializa o cliente HTTP.
     */
    public NewsFetcher() {
        this(false);
    }

    /**
     * Construtor que inicializa o cliente HTTP com opção de SSL permissivo.
     *
     * @param disableSslVerification Se true, desabilita verificação de certificados SSL
     */
    public NewsFetcher(boolean disableSslVerification) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (disableSslVerification) {
            try {
                SSLContext sslContext = createTrustAllSslContext();
                builder.sslContext(sslContext);
            } catch (Exception e) {
                System.err.println("⚠️ Erro ao configurar SSL permissivo no NewsFetcher: " + e.getMessage());
            }
        }

        this.httpClient = builder.build();
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     * ATENÇÃO: Use apenas em ambientes de desenvolvimento/teste.
     */
    private SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Aceita todos os certificados
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Aceita todos os certificados
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    /**
     * Busca notícias para um ticker de FII específico.
     *
     * A busca é feita usando o padrão "TICKER FII" para melhorar
     * a precisão dos resultados (ex: "HGLG11 FII").
     *
     * @param ticker Código do fundo imobiliário (ex: HGLG11)
     * @param maxResults Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchNews(String ticker, int maxResults) {
        return fetchNewsByQuery(ticker + " FII", maxResults);
    }

    /**
     * Busca notícias de criptomoedas usando o nome e símbolo.
     *
     * @param symbol Símbolo da crypto (ex: BTC)
     * @param name   Nome completo (ex: Bitcoin)
     * @param maxResults Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchCryptoNews(String symbol, String name, int maxResults) {
        // Usa o nome completo + "crypto" para melhores resultados
        String query = name + " " + symbol + " crypto";
        return fetchNewsByQuery(query, maxResults);
    }

    /**
     * Busca notícias de ações brasileiras usando o ticker.
     *
     * @param ticker Código da ação (ex: PETR3, VALE3)
     * @param maxResults Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchStockNews(String ticker, int maxResults) {
        // Usa o ticker + "ação" para melhores resultados no Google News
        String query = ticker + " ação bolsa";
        return fetchNewsByQuery(query, maxResults);
    }

    /**
     * Busca notícias de ações americanas usando o ticker.
     * Retorna notícias em inglês para maior cobertura.
     *
     * @param ticker     Código da ação (ex: AAPL, MSFT, GOOGL)
     * @param maxResults Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchUSStockNews(String ticker, int maxResults) {
        // Busca em inglês para maior volume de artigos
        String query = ticker + " stock";
        return fetchNewsByQueryEnglish(query, maxResults);
    }

    /**
     * Busca notícias usando uma query no Google News RSS em inglês (US).
     *
     * @param searchQuery Query de busca em inglês
     * @param maxResults  Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchNewsByQueryEnglish(String searchQuery, int maxResults) {
        List<NewsItem> newsItems = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

            // URL em inglês para notícias dos EUA
            String url = String.format(
                    "%s?q=%s&hl=en-US&gl=US&ceid=US:en",
                    GOOGLE_NEWS_RSS_URL,
                    encodedQuery
            );

            System.out.println("🌐 Buscando (EN): " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (compatible; FIINewsBot/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠️ Status HTTP: " + response.statusCode());
                return newsItems;
            }

            newsItems = parseRssXml(response.body(), maxResults);

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro na requisição HTTP: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar notícias: " + e.getMessage());
        }

        return newsItems;
    }

    /**
     * Busca notícias usando uma query genérica no Google News RSS.
     *
     * @param searchQuery Query de busca (ex: "Bitcoin BTC crypto", "HGLG11 FII")
     * @param maxResults  Número máximo de notícias a retornar
     * @return Lista de NewsItem com título e link das notícias
     */
    public List<NewsItem> fetchNewsByQuery(String searchQuery, int maxResults) {
        List<NewsItem> newsItems = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

            // Constrói a URL completa do RSS
            // hl=pt-BR: Idioma português do Brasil
            // gl=BR: Localização Brasil
            // ceid=BR:pt-419: Configuração de região/idioma
            String url = String.format(
                    "%s?q=%s&hl=pt-BR&gl=BR&ceid=BR:pt-419",
                    GOOGLE_NEWS_RSS_URL,
                    encodedQuery
            );

            System.out.println("🌐 Buscando: " + url);

            // Faz a requisição HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (compatible; FIINewsBot/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠️ Status HTTP: " + response.statusCode());
                return newsItems;
            }

            // Faz o parsing do XML RSS usando Jsoup
            newsItems = parseRssXml(response.body(), maxResults);

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erro na requisição HTTP: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar notícias: " + e.getMessage());
        }

        return newsItems;
    }

    /**
     * Faz o parsing do XML RSS do Google News e extrai as notícias.
     *
     * Estrutura do RSS:
     * <rss>
     *   <channel>
     *     <item>
     *       <title>Título da notícia</title>
     *       <link>URL da notícia</link>
     *       <pubDate>Data de publicação</pubDate>
     *     </item>
     *     ...
     *   </channel>
     * </rss>
     *
     * @param xmlContent Conteúdo XML do feed RSS
     * @param maxResults Número máximo de itens a extrair
     * @return Lista de NewsItem com as notícias extraídas
     */
    private List<NewsItem> parseRssXml(String xmlContent, int maxResults) {
        List<NewsItem> newsItems = new ArrayList<>();

        try {
            // Usa Jsoup com parser XML
            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());

            // Seleciona todos os itens do feed
            Elements items = doc.select("item");

            // Data limite: 1 mês atrás
            ZonedDateTime oneMonthAgo = ZonedDateTime.now().minus(1, ChronoUnit.MONTHS);

            int count = 0;
            for (Element item : items) {
                if (count >= maxResults) {
                    break;
                }

                // Extrai título, link e data de publicação
                String title = item.select("title").text();
                String link = item.select("link").text();
                String pubDateStr = item.select("pubDate").text();

                // Filtra por data: ignora notícias com mais de 1 mês
                if (pubDateStr != null && !pubDateStr.isEmpty()) {
                    try {
                        // Formato do Google News RSS: "Thu, 20 Feb 2026 14:30:00 GMT"
                        ZonedDateTime pubDate = ZonedDateTime.parse(pubDateStr,
                                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH));
                        if (pubDate.isBefore(oneMonthAgo)) {
                            System.out.println("⏭️ Notícia ignorada (mais de 1 mês): " + pubDateStr);
                            continue;
                        }
                    } catch (Exception e) {
                        // Se não conseguir parsear a data, inclui a notícia por precaução
                        System.err.println("⚠️ Não foi possível parsear data: " + pubDateStr);
                    }
                }

                // Verifica se os campos são válidos
                if (title != null && !title.isEmpty() && link != null && !link.isEmpty()) {
                    // Limpa o título (remove fonte que às vezes vem no final)
                    title = cleanTitle(title);

                    newsItems.add(new NewsItem(title, link));
                    count++;
                }
            }

            System.out.println("📰 Encontradas " + newsItems.size() + " notícias (filtro: último mês)");

        } catch (Exception e) {
            System.err.println("❌ Erro ao fazer parsing do RSS: " + e.getMessage());
        }

        return newsItems;
    }

    /**
     * Limpa o título da notícia removendo a fonte que às vezes
     * aparece no final no formato " - Nome da Fonte".
     *
     * @param title Título original
     * @return Título limpo
     */
    private String cleanTitle(String title) {
        if (title == null) return "";

        // O Google News às vezes adiciona " - Fonte" no final do título
        // Limitamos o tamanho para melhor exibição
        if (title.length() > 150) {
            title = title.substring(0, 147) + "...";
        }

        return title.trim();
    }
}


