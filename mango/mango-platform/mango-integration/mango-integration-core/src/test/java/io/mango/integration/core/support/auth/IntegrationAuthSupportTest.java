package io.mango.integration.support.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("集成认证工具类测试")
class IntegrationAuthSupportTest {

    private HttpServer server;
    private final Map<String, String> capturedHeaders = new HashMap<>();
    private int tokenRequestCount;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenRequestCount++;
            byte[] body = "{\"access_token\":\"token-123\",\"expires_in\":3600}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api", exchange -> {
            exchange.getRequestHeaders().forEach((name, values) ->
                    capturedHeaders.put(name.toLowerCase(Locale.ROOT), values.get(0)));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("API Key 认证写入声明的请求头")
    void apiKeyAuthAppliesHeader() throws Exception {
        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                "{\"authMode\":\"API_KEY\",\"params\":{\"headerName\":\"X-Api-Key\"}}",
                Map.of("appKey", "key-abc"));
        URI uri = URI.create(baseUrl() + "/api");
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).GET();

        IntegrationOutboundAuth.of(config, null).apply(builder, "GET", uri);
        send(builder.build());

        assertThat(capturedHeaders).containsEntry("x-api-key", "key-abc");
    }

    @Test
    @DisplayName("OAuth2 客户端凭证获取令牌并复用缓存")
    void oauth2FetchesTokenAndCaches() throws Exception {
        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                "{\"authMode\":\"OAUTH2\",\"params\":{\"tokenUrl\":\"" + baseUrl() + "/token\","
                        + "\"clientIdField\":\"clientId\",\"clientSecretField\":\"clientSecret\"}}",
                Map.of("clientId", "cid", "clientSecret", "secret"));
        IntegrationTokenCache cache = new IntegrationTokenCache();
        IntegrationOutboundAuth auth = IntegrationOutboundAuth.of(config, cache);

        for (int i = 0; i < 2; i++) {
            URI uri = URI.create(baseUrl() + "/api");
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri).GET();
            auth.apply(builder, "GET", uri);
            send(builder.build());
        }

        assertThat(capturedHeaders).containsEntry("authorization", "Bearer token-123");
        assertThat(tokenRequestCount).isEqualTo(1);
    }

    @Test
    @DisplayName("HMAC 签名认证写入时间戳和可验证签名")
    void hmacAuthSignsRequest() throws Exception {
        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                "{\"authMode\":\"HMAC_SIGNATURE\",\"params\":{\"algorithm\":\"HmacSHA256\","
                        + "\"timestampHeader\":\"X-Timestamp\",\"signatureHeader\":\"X-Signature\","
                        + "\"signedContent\":\"METHOD\\nPATH\\nTIMESTAMP\"}}",
                Map.of("appSecret", "shared-secret"));
        URI uri = URI.create(baseUrl() + "/api");

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).GET();
        IntegrationOutboundAuth.of(config, null).apply(builder, "GET", uri);
        send(builder.build());

        String timestamp = capturedHeaders.get("x-timestamp");
        String expected = hmac("shared-secret", "GET\n/api\n" + timestamp);
        assertThat(capturedHeaders.get("x-signature")).isEqualTo(expected);
    }

    @Test
    @DisplayName("回调签名校验可识别篡改")
    void inboundSignatureVerificationDetectsTampering() {
        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                "{\"authMode\":\"CALLBACK_SIGNATURE\",\"params\":{\"algorithm\":\"HmacSHA256\","
                        + "\"signatureParam\":\"signature\",\"timestampParam\":\"timestamp\","
                        + "\"nonceParam\":\"nonce\",\"signedContent\":\"SECRET\\nTIMESTAMP\\nNONCE\\nBODY\"}}",
                Map.of("callbackSecret", "callback-secret"));
        IntegrationInboundAuth auth = IntegrationInboundAuth.of(config);

        assertThat(auth.verifySignature(signedParams("body"), "body")).isTrue();
        assertThat(auth.verifySignature(signedParams("body"), "tampered")).isFalse();
    }

    @Test
    @DisplayName("AES 回调报文可解密")
    void inboundDecryptsAesPayload() throws Exception {
        byte[] key = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                "{\"authMode\":\"CALLBACK_SIGNATURE\",\"params\":{\"encryptionEnabled\":\"true\","
                        + "\"keyField\":\"encodingAesKey\"}}",
                Map.of("encodingAesKey", Base64.getEncoder().encodeToString(key)));
        String encrypted = encrypt(key, "{\"event\":\"test\"}");

        String plain = IntegrationInboundAuth.of(config).decrypt(encrypted);

        assertThat(plain).isEqualTo("{\"event\":\"test\"}");
    }

    private Map<String, String> signedParams(String body) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = hmac("callback-secret", "callback-secret\n" + timestamp + "\nn1\n" + body);
        return Map.of("timestamp", timestamp, "nonce", "n1", "signature", signature);
    }

    private void send(HttpRequest request) throws Exception {
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(request, HttpResponse.BodyHandlers.discarding());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encrypt(byte[] key, String plain) throws Exception {
        byte[] iv = "abcdefghijklmnop".getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] data = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[16 + data.length];
        System.arraycopy(iv, 0, payload, 0, 16);
        System.arraycopy(data, 0, payload, 16, data.length);
        return Base64.getEncoder().encodeToString(payload);
    }
}
