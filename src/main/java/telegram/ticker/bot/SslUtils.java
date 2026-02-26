package telegram.ticker.bot;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Classe utilitária para configuração de SSL.
 * Permite criar HttpClients que aceitam qualquer certificado SSL.
 *
 * ATENÇÃO: Use apenas em ambientes de desenvolvimento ou redes corporativas
 * com proxies que interceptam tráfego HTTPS.
 */
public class SslUtils {

    /**
     * Cria um HttpClient do Apache que aceita qualquer certificado SSL.
     *
     * @return CloseableHttpClient configurado para aceitar todos os certificados
     */
    public static CloseableHttpClient createTrustAllHttpClient() {
        try {
            // Cria SSLContext que confia em todos os certificados
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            // Cria factory de sockets SSL com hostname verifier permissivo
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            // Constrói o HttpClient
            return HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setSSLContext(sslContext)
                    .build();

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao criar HttpClient com SSL permissivo: " + e.getMessage());
            // Retorna um cliente padrão se falhar
            return HttpClients.createDefault();
        }
    }

    /**
     * Cria um SSLContext que confia em todos os certificados.
     *
     * @return SSLContext configurado para aceitar todos os certificados
     */
    public static SSLContext createTrustAllSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Aceita todos
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Aceita todos
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao criar SSLContext permissivo: " + e.getMessage());
            return null;
        }
    }
}

