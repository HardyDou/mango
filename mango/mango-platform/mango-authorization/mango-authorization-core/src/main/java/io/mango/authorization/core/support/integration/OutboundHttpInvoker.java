package io.mango.authorization.core.support.integration;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * 出站可达性探测执行器。
 *
 * <p>只发起无认证 GET 请求，用于静态入口与健康地址探测；协议级调用由连接器适配器负责。</p>
 */
public class OutboundHttpInvoker {

    private static final int MAX_MESSAGE_LENGTH = 200;

    private final boolean allowPrivateNetwork;

    public OutboundHttpInvoker(boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
    }

    /**
     * 执行可达性探测。
     *
     * @param request 探测描述
     * @return 探测结果，不含响应正文与凭证
     */
    public OutboundResult invoke(OutboundCall request) {
        long startedAt = System.nanoTime();
        URI uri;
        try {
            uri = resolveUri(request);
        } catch (IllegalArgumentException exception) {
            return failure(startedAt, exception.getMessage());
        }
        if (!allowPrivateNetwork && !isPublicTarget(uri.getHost())) {
            return failure(startedAt, "目标地址不在允许范围内");
        }
        int attempts = 1 + Math.max(request.getRetryMax(), 0);
        OutboundResult result = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            result = executeOnce(request, uri, startedAt);
            if (result.isSuccess() || attempt == attempts - 1) {
                return result;
            }
            sleepBeforeRetry(attempt);
            startedAt = System.nanoTime();
        }
        return result == null ? failure(startedAt, "请求未执行") : result;
    }

    private OutboundResult executeOnce(OutboundCall request, URI uri, long startedAt) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMillis(request.getTimeoutMs()))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(request.getTimeoutMs()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpResponse<Void> response = client.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            long elapsed = elapsedMs(startedAt);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 400;
            return OutboundResult.builder()
                    .success(success)
                    .statusCode(response.statusCode())
                    .elapsedMs(elapsed)
                    .message(success ? "连接成功" : "目标返回非成功状态码")
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(startedAt, "连接被中断");
        } catch (Exception exception) {
            return failure(startedAt, "连接失败：" + exception.getClass().getSimpleName());
        }
    }

    private URI resolveUri(OutboundCall request) {
        String baseUrl = request.getBaseUrl() == null ? "" : request.getBaseUrl().trim();
        if (baseUrl.isEmpty()) {
            throw new IllegalArgumentException("目标地址不能为空");
        }
        URI base = URI.create(baseUrl);
        String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("目标地址必须是http或https");
        }
        if (base.getHost() == null || base.getHost().isBlank()) {
            throw new IllegalArgumentException("目标地址缺少主机");
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = request.getPath() == null ? "" : request.getPath().trim();
        if (path.isEmpty() || "/".equals(path)) {
            return URI.create(normalizedBase);
        }
        return URI.create(normalizedBase + (path.startsWith("/") ? path : "/" + path));
    }

    private boolean isPublicTarget(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return !(address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isMulticastAddress());
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(150L * (attempt + 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private OutboundResult failure(long startedAt, String message) {
        return OutboundResult.builder()
                .success(false)
                .statusCode(null)
                .elapsedMs(elapsedMs(startedAt))
                .message(truncate(message))
                .build();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
