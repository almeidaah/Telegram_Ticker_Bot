package com.scibite.pipelines;

import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import io.github.cdimascio.dotenv.Dotenv;

import javax.net.ssl.*;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Classe principal que inicializa o bot do Telegram.
 *
 * Este bot recebe tickers de ações, fundos imobiliários e criptomoedas
 * e retorna indicadores financeiros e notícias recentes usando o Google News RSS.
 *
 * Configuração necessária no arquivo .env:
 * - TELEGRAM_BOT_TOKEN: Token do bot fornecido pelo BotFather
 * - TELEGRAM_BOT_USERNAME: Nome de usuário do bot no Telegram
 * - DISABLE_SSL_VERIFICATION: (opcional) Defina como "true" para desabilitar
 *   verificação SSL em ambientes corporativos com proxy/firewall
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("🚀 Iniciando Telegram Ticker Bot...");

        try {
            // Carrega variáveis de ambiente do arquivo .env
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Verifica se deve desabilitar verificação SSL (para ambientes corporativos)
            String disableSsl = dotenv.get("DISABLE_SSL_VERIFICATION");
            boolean disableSslVerification = "true".equalsIgnoreCase(disableSsl);

            if (disableSslVerification) {
                System.out.println("⚠️ Verificação SSL desabilitada (ambiente corporativo)");
                disableSslVerificationGlobally();
            }

            // Obtém as credenciais do bot
            String botToken = dotenv.get("TELEGRAM_BOT_TOKEN");
            String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

            // Valida se as credenciais foram configuradas
            if (botToken == null || botToken.isEmpty()) {
                System.err.println("❌ Erro: TELEGRAM_BOT_TOKEN não configurado no arquivo .env");
                System.exit(1);
            }

            if (botUsername == null || botUsername.isEmpty()) {
                System.err.println("❌ Erro: TELEGRAM_BOT_USERNAME não configurado no arquivo .env");
                System.exit(1);
            }

            // Inicializa a API do Telegram Bots
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Registra o bot (passa flag de SSL)
            TelegramTickerBot tickerBot = new TelegramTickerBot(botToken, botUsername, disableSslVerification);

            // Se SSL desabilitado, injeta HttpClient trust-all no bot via reflection
            // (a lib TelegramBots usa Apache HttpClient internamente e ignora SSLContext.setDefault)
            if (disableSslVerification) {
                injectTrustAllHttpClient(tickerBot);
            }

            BotSession session = botsApi.registerBot(tickerBot);

            // Também injeta HttpClient trust-all no ReaderThread do long-polling
            if (disableSslVerification) {
                injectTrustAllIntoSession(session);
            }

            System.out.println("✅ Bot iniciado com sucesso!");
            System.out.println("📱 Username: @" + botUsername);
            System.out.println("📰 Fonte de notícias: Google News RSS");
            System.out.println("⏳ Aguardando mensagens...");

        } catch (TelegramApiException e) {
            System.err.println("❌ Erro ao inicializar o bot do Telegram: " + e.getMessage());
            System.err.println("");
            System.err.println("💡 Dicas de solução:");
            System.err.println("   1. Verifique se o token do bot está correto no arquivo .env");
            System.err.println("   2. Se estiver em rede corporativa com proxy/firewall, adicione:");
            System.err.println("      DISABLE_SSL_VERIFICATION=true no arquivo .env");
            System.err.println("   3. Verifique sua conexão com a internet");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Desabilita a verificação de certificado SSL globalmente.
     * USE APENAS em ambientes de desenvolvimento/teste com proxies corporativos.
     * Isso afeta todas as conexões HTTPS do aplicativo.
     */
    private static void disableSslVerificationGlobally() {
        try {
            // Cria um TrustManager que aceita todos os certificados
            TrustManager[] trustAllCerts = new TrustManager[] {
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

            // Instala o TrustManager permissivo
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Desabilita verificação de hostname
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            // Define o SSLContext padrão para todo o aplicativo
            SSLContext.setDefault(sc);

            // Configura propriedades do sistema para Apache HttpClient
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            System.err.println("⚠️ Erro ao desabilitar verificação SSL: " + e.getMessage());
        }
    }

    /**
     * Injeta um HttpClient trust-all no DefaultAbsSender (usado para chamadas à API do Telegram).
     */
    private static void injectTrustAllHttpClient(TelegramTickerBot bot) {
        try {
            Field httpClientField = DefaultAbsSender.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(bot, SslUtils.createTrustAllHttpClient());
            System.out.println("🔓 HttpClient trust-all injetado no bot (API calls)");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("⚠️ Erro ao injetar HttpClient trust-all no bot: " + e.getMessage());
        }
    }

    /**
     * Injeta um HttpClient trust-all no ReaderThread do DefaultBotSession (usado para long-polling).
     */
    private static void injectTrustAllIntoSession(BotSession session) {
        try {
            // Acessa o campo readerThread no DefaultBotSession
            Field readerThreadField = DefaultBotSession.class.getDeclaredField("readerThread");
            readerThreadField.setAccessible(true);
            Object readerThread = readerThreadField.get(session);

            if (readerThread != null) {
                // Acessa o campo httpclient dentro do ReaderThread
                Field rtHttpClientField = readerThread.getClass().getDeclaredField("httpclient");
                rtHttpClientField.setAccessible(true);
                rtHttpClientField.set(readerThread, SslUtils.createTrustAllHttpClient());
                System.out.println("🔓 HttpClient trust-all injetado no long-polling");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("⚠️ Erro ao injetar HttpClient trust-all no session: " + e.getMessage());
        }
    }
}
