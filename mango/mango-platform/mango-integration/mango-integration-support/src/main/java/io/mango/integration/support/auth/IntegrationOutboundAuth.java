package io.mango.integration.support.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * 出站认证工具。
 *
 * <p>按连接器认证规范为请求追加认证信息，业务域不需要自行实现签名、令牌或请求头拼装。</p>
 */
public final class IntegrationOutboundAuth {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final IntegrationAuthConfig config;
    private final IntegrationTokenCache tokenCache;

    private IntegrationOutboundAuth(IntegrationAuthConfig config, IntegrationTokenCache tokenCache) {
        this.config = config;
        this.tokenCache = tokenCache;
    }

    /**
     * 创建出站认证工具。
     *
     * @param config 认证配置
     * @param tokenCache 令牌缓存，可复用同一实例
     * @return 出站认证工具
     */
    public static IntegrationOutboundAuth of(IntegrationAuthConfig config, IntegrationTokenCache tokenCache) {
        return new IntegrationOutboundAuth(config, tokenCache == null ? new IntegrationTokenCache() : tokenCache);
    }

    /**
     * 为请求追加认证信息。
     *
     * @param builder 请求构建器
     * @param method HTTP 方法
     * @param uri 目标地址
     * @return 追加认证后的请求构建器
     */
    public HttpRequest.Builder apply(HttpRequest.Builder builder, String method, URI uri) {
        String mode = config.authMode().toUpperCase(Locale.ROOT);
        switch (mode) {
            case "NONE" -> {
                return builder;
            }
            case "API_KEY" -> {
                String header = config.param("headerName", "X-Api-Key");
                return builder.header(header, config.credential(config.param("secretField", null)));
            }
            case "SECRET", "BEARER_TOKEN" -> {
                return builder.header(HEADER_AUTHORIZATION,
                        "Bearer " + config.credential(config.param("secretField", null)));
            }
            case "BASIC" -> {
                String username = config.param("usernameField", "username");
                String password = config.param("passwordField", "password");
                String raw = config.optionalCredential(username) != null
                        ? config.optionalCredential(username) + ":" + config.optionalCredential(password)
                        : config.credential(null);
                return builder.header(HEADER_AUTHORIZATION,
                        "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            }
            case "OAUTH2" -> {
                String token = tokenCache.token(config);
                return builder.header(HEADER_AUTHORIZATION, "Bearer " + token);
            }
            case "HMAC_SIGNATURE" -> {
                return applyHmac(builder, method, uri);
            }
            default -> throw new IllegalStateException("不支持的认证方式：" + config.authMode());
        }
    }

    /**
     * 生成带认证参数的查询串，用于把令牌放在 URL 上的场景。
     *
     * @param baseUri 原始地址
     * @return 追加认证参数后的地址
     */
    public URI applyQuery(URI baseUri) {
        if (!"QUERY_TOKEN".equalsIgnoreCase(config.authMode())) {
            return baseUri;
        }
        String token = tokenCache.token(config);
        String param = config.param("tokenParam", "access_token");
        String separator = baseUri.getRawQuery() == null ? "?" : "&";
        return URI.create(baseUri + separator + param + "=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder applyHmac(HttpRequest.Builder builder, String method, URI uri) {
        String algorithm = config.param("algorithm", "HmacSHA256");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String template = config.param("signedContent", "METHOD\nPATH\nTIMESTAMP");
        String payload = template
                .replace("METHOD", method.toUpperCase(Locale.ROOT))
                .replace("PATH", uri.getRawPath())
                .replace("TIMESTAMP", timestamp);
        String secret = config.credential(config.param("secretField", null));
        String signature = hmacHex(algorithm, secret, payload);
        builder.header(config.param("timestampHeader", "X-Timestamp"), timestamp);
        return builder.header(config.param("signatureHeader", "X-Signature"), signature);
    }

    private String hmacHex(String algorithm, String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败");
        }
    }
}
