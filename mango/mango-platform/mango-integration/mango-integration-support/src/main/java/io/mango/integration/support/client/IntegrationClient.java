package io.mango.integration.support.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.integration.api.vo.IntegrationCredentialFieldVO;
import io.mango.integration.api.spi.IntegrationDeclarationProvider;
import io.mango.integration.api.vo.IntegrationInstanceDeclarationVO;
import io.mango.integration.support.auth.IntegrationAuthConfig;
import io.mango.integration.support.auth.IntegrationOutboundAuth;
import io.mango.integration.support.auth.IntegrationTokenCache;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 集成调用客户端。
 *
 * <p>按实例声明读取配置与凭证，自动应用认证、超时与重试，业务域不需要自行拼装请求。</p>
 */
public final class IntegrationClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL_FIELD = "apiBaseUrl";
    private static final String[] BASE_URL_FALLBACKS = {"baseUrl", "endpoint", "gatewayBaseUrl"};
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_RETRY_MAX = 0;
    private static final int MAX_BODY_LENGTH = 4000;

    private final String baseUrl;
    private final int timeoutMs;
    private final int retryMax;
    private final IntegrationOutboundAuth auth;
    private final HttpClient httpClient;
    private final String instanceCode;
    private final String connectorCode;
    private final IntegrationCallRecorder recorder;

    private IntegrationClient(String baseUrl, int timeoutMs, int retryMax, IntegrationOutboundAuth auth,
                              String instanceCode, String connectorCode, IntegrationCallRecorder recorder) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryMax = retryMax;
        this.auth = auth;
        this.instanceCode = instanceCode;
        this.connectorCode = connectorCode;
        this.recorder = recorder;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * 按实例声明创建客户端。
     *
     * @param provider 声明读取能力
     * @param instanceCode 实例编码
     * @param tokenCache 令牌缓存，可复用同一实例
     * @return 集成调用客户端
     */
    public static IntegrationClient of(IntegrationDeclarationProvider provider, String instanceCode,
                                       IntegrationTokenCache tokenCache) {
        return of(provider, instanceCode, tokenCache, null);
    }

    /**
     * 按实例声明创建带调用记录的客户端。
     *
     * @param provider 声明读取能力
     * @param instanceCode 实例编码
     * @param tokenCache 令牌缓存
     * @param recorder 调用记录写入能力，可为空
     * @return 集成调用客户端
     */
    public static IntegrationClient of(IntegrationDeclarationProvider provider, String instanceCode,
                                       IntegrationTokenCache tokenCache, IntegrationCallRecorder recorder) {
        IntegrationInstanceDeclarationVO instance = provider.instance(instanceCode);
        Map<String, String> configValues = parseConfig(instance.getConfigValuesJson());
        String baseUrl = resolveBaseUrl(configValues);
        int timeoutMs = intValue(configValues.get("timeoutMs"), DEFAULT_TIMEOUT_MS);
        int retryMax = intValue(configValues.get("retryMax"), DEFAULT_RETRY_MAX);
        Map<String, String> credentials = new LinkedHashMap<>();
        List<IntegrationCredentialFieldVO> fields = instance.getCredentialCode() == null
                ? List.of() : provider.credentials(instance.getCredentialCode());
        fields.forEach(field -> credentials.put(field.getFieldCode(), field.getFieldValue()));
        IntegrationAuthConfig authConfig = IntegrationAuthConfig.of(
                provider.connectorAuthSchema(instance.getConnectorCode()), credentials);
        return new IntegrationClient(baseUrl, timeoutMs, retryMax,
                IntegrationOutboundAuth.of(authConfig, tokenCache),
                instance.getInstanceCode(), instance.getConnectorCode(), recorder);
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 相对路径
     * @return 调用结果
     */
    public IntegrationCallResult get(String path) {
        return execute("GET", path, null);
    }

    /**
     * 发送 POST 请求。
     *
     * @param path 相对路径
     * @param body 请求体
     * @return 调用结果
     */
    public IntegrationCallResult post(String path, String body) {
        return execute("POST", path, body);
    }

    private IntegrationCallResult execute(String method, String path, String body) {
        long startedAt = System.nanoTime();
        URI uri;
        try {
            uri = URI.create(normalizeBaseUrl() + normalizePath(path));
        } catch (IllegalArgumentException exception) {
            return failure(startedAt, "目标地址不合法");
        }
        int attempts = 1 + Math.max(retryMax, 0);
        IntegrationCallResult result = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            result = executeOnce(method, uri, body, startedAt);
            if (result.isSuccess() || attempt == attempts - 1) {
                break;
            }
            startedAt = System.nanoTime();
        }
        IntegrationCallResult finalResult = result == null ? failure(startedAt, "请求未执行") : result;
        recordCall(method, path, finalResult);
        return finalResult;
    }

    private IntegrationCallResult executeOnce(String method, URI uri, String body, long startedAt) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMillis(timeoutMs));
            if ("POST".equalsIgnoreCase(method)) {
                builder.header("Content-Type", "application/json");
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            } else {
                builder.GET();
            }
            auth.apply(builder, method, uri);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long elapsed = elapsedMs(startedAt);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return IntegrationCallResult.builder()
                    .success(success)
                    .statusCode(response.statusCode())
                    .elapsedMs(elapsed)
                    .body(truncate(response.body()))
                    .message(success ? "调用成功" : "目标返回非成功状态码")
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(startedAt, "调用被中断");
        } catch (Exception exception) {
            return failure(startedAt, "调用失败：" + exception.getClass().getSimpleName());
        }
    }

    private String normalizeBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String resolveBaseUrl(Map<String, String> configValues) {
        String value = configValues.get(DEFAULT_BASE_URL_FIELD);
        if (value != null && !value.isBlank()) {
            return value;
        }
        for (String field : BASE_URL_FALLBACKS) {
            String candidate = configValues.get(field);
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        throw new IllegalStateException("实例未配置接口地址");
    }

    private static Map<String, String> parseConfig(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            root.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
            return values;
        } catch (Exception exception) {
            throw new IllegalStateException("实例配置不是合法JSON");
        }
    }

    private static int intValue(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private void recordCall(String method, String path, IntegrationCallResult result) {
        if (recorder == null) {
            return;
        }
        recorder.record(IntegrationCallRecord.builder()
                .instanceCode(instanceCode)
                .connectorCode(connectorCode)
                .method(method)
                .path(path)
                .statusCode(result.getStatusCode())
                .elapsedMs(result.getElapsedMs())
                .success(result.isSuccess())
                .message(result.getMessage())
                .build());
    }

    private IntegrationCallResult failure(long startedAt, String message) {
        return IntegrationCallResult.builder()
                .success(false)
                .statusCode(0)
                .elapsedMs(elapsedMs(startedAt))
                .message(message)
                .build();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH);
    }
}
