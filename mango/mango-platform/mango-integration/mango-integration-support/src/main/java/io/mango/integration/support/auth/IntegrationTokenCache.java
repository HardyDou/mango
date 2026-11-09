package io.mango.integration.support.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 出站访问令牌缓存。
 *
 * <p>按令牌接口地址与凭证指纹缓存，过期前复用；令牌不落库、不写日志。</p>
 */
public final class IntegrationTokenCache {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long REFRESH_MARGIN_SECONDS = 30L;

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 获取访问令牌，命中缓存时直接返回。
     *
     * @param config 认证配置
     * @return 访问令牌
     */
    public String token(IntegrationAuthConfig config) {
        String tokenUrl = config.param("tokenUrl", null);
        if (tokenUrl == null || tokenUrl.isBlank()) {
            throw new IllegalStateException("未配置令牌接口地址");
        }
        String clientId = config.param("clientIdField", "clientId");
        String secretField = config.param("clientSecretField", "clientSecret");
        String cacheKey = tokenUrl + "|" + config.optionalCredential(clientId) + "|"
                + Integer.toHexString(String.valueOf(config.optionalCredential(secretField)).hashCode());
        Cached cached = cache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.token();
        }
        Cached fetched = fetch(config, tokenUrl, clientId, secretField);
        cache.put(cacheKey, fetched);
        return fetched.token();
    }

    /**
     * 清除缓存，凭证轮换后调用。
     */
    public void clear() {
        cache.clear();
    }

    private Cached fetch(IntegrationAuthConfig config, String tokenUrl, String clientIdField, String secretField) {
        StringBuilder body = new StringBuilder();
        append(body, config.param("clientIdParam", "client_id"), config.optionalCredential(clientIdField));
        append(body, config.param("clientSecretParam", "client_secret"), config.optionalCredential(secretField));
        String scope = config.param("scope", null);
        if (scope != null && !scope.isBlank()) {
            append(body, "scope", scope);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("令牌接口返回非成功状态");
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String tokenField = config.param("tokenField", "access_token");
            String token = root.path(tokenField).asText(null);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("令牌响应缺少令牌字段");
            }
            long expiresIn = root.path(config.param("expiresField", "expires_in")).asLong(0L);
            long ttl = Math.max(expiresIn - REFRESH_MARGIN_SECONDS, 0L);
            return new Cached(token, System.currentTimeMillis() + ttl * 1000L);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("令牌获取被中断");
        } catch (Exception exception) {
            throw new IllegalStateException("令牌获取失败：" + exception.getClass().getSimpleName());
        }
    }

    private void append(StringBuilder body, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (body.length() > 0) {
            body.append('&');
        }
        body.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private record Cached(String token, long expiresAtMillis) {
        boolean expired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
    }
}
