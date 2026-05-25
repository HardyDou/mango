package io.mango.infra.web.filter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.infra.web.starter.MangoWebProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * 内部调用过滤器，拦截对内部 API 的直接访问。
 *
 * <p>流量架构：
 * <ul>
 *   <li>外部请求 → 必须经过 Gateway</li>
 *   <li>微服务间调用 → Feign 携带 X-Internal-Call 和签名 Header</li>
 *   <li>直接访问微服务 → 本过滤器拦截并拒绝</li>
 * </ul>
 *
 * <p>安全机制：
 * <ul>
 *   <li>HMAC-SHA256 签名防伪造</li>
 *   <li>时间戳偏差检查（5分钟内）</li>
 *   <li>Nonce 黑名单防重放</li>
 * </ul>
 *
 * @author Mango
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalCallFilter implements Filter {

    private final IInternalPathProvider internalPathProvider;
    private final IKvStore kvStore;
    private final MangoWebProperties properties;

    /**
     * 从内部路径提供器加载的内部路径集合。
     */
    private final Set<String> internalPaths = new CopyOnWriteArraySet<>();

    /**
     * 标记内部路径是否已成功加载。
     */
    private volatile boolean pathsLoaded = false;

    /**
     * 最近一次成功加载内部路径的时间戳。
     */
    private volatile long lastKnownPathsLoadTime = 0;

    /**
     * 应用就绪后加载内部路径，启动加载失败时进入安全拒绝模式。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            loadInternalPaths();
            this.pathsLoaded = true;
            this.lastKnownPathsLoadTime = System.currentTimeMillis();
            log.info("Internal paths loaded successfully: count={}", internalPaths.size());
        } catch (Exception e) {
            this.pathsLoaded = false;
            log.error("Failed to load internal paths at startup, rejecting all requests", e);
        }
    }

    /**
     * 定时刷新内部路径；刷新失败时保留上一次可用结果。
     */
    @Scheduled(fixedRateString = "${mango.web.inner.path-refresh-interval-seconds:300}000")
    public void refreshInternalPaths() {
        try {
            loadInternalPaths();
            this.pathsLoaded = true;
            this.lastKnownPathsLoadTime = System.currentTimeMillis();
            log.info("Internal paths refreshed: count={}", internalPaths.size());
        } catch (Exception e) {
            log.warn("Failed to refresh internal paths, keeping last known paths", e);
            // 保留上一次已知路径，pathsLoaded 仍保持 true。
        }
    }

    /**
     * 从提供器加载内部路径。
     */
    private void loadInternalPaths() {
        List<String> paths = internalPathProvider.getInternalPaths();
        if (paths != null) {
            this.internalPaths.clear();
            this.internalPaths.addAll(paths);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!properties.getInner().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 非内部路径直接放行。
        if (!isInternalPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 安全拒绝：启动时路径未加载成功，则拒绝内部路径请求。
        if (!pathsLoaded) {
            sendForbidden(response, "Internal paths not loaded");
            return;
        }

        // 检查 X-Internal-Call Header。
        String internalCall = request.getHeader("X-Internal-Call");
        if (!"true".equals(internalCall)) {
            sendForbidden(response, "Missing X-Internal-Call header");
            return;
        }

        // 准备校验签名。
        String sharedSecret = sharedSecret();
        if (!StringUtils.hasText(sharedSecret)) {
            log.error("No internal call secret configured, rejecting internal request");
            sendForbidden(response, "Internal call secret is not configured");
            return;
        }

        // 校验时间戳。
        String timestampStr = request.getHeader("X-Internal-Timestamp");
        if (!verifyTimestamp(timestampStr)) {
            sendForbidden(response, "Invalid or expired timestamp");
            return;
        }

        // 校验 nonce，防止重放。
        String nonce = request.getHeader("X-Internal-Nonce");
        if (!verifyNonce(nonce)) {
            sendForbidden(response, "Nonce already used");
            return;
        }

        // 校验签名。
        String signature = request.getHeader("X-Internal-Signature");
        String secretVersion = request.getHeader("X-Internal-Secret-Version");
        if (!verifySignature(request, timestampStr, nonce, secretVersion, signature)) {
            sendForbidden(response, "Invalid signature");
            return;
        }

        // nonce 通过校验后写入黑名单。
        if (StringUtils.hasText(nonce)) {
            try {
                kvStore.setIfAbsent("nonce:" + nonce, "1", nonceTtlSeconds());
            } catch (Exception e) {
                log.warn("Failed to add nonce to blacklist", e);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 判断路径是否为内部路径。
     */
    private boolean isInternalPath(String path) {
        for (String pattern : internalPaths) {
            if (matchPath(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 支持通配符的路径匹配。
     */
    private boolean matchPath(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1));
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            int slashIndex = path.indexOf('/', prefix.length());
            return path.startsWith(prefix) && (slashIndex == -1 || slashIndex == path.length() - 1);
        }
        return false;
    }

    /**
     * 校验时间戳是否在容忍范围内；只接受过去时间，不接受未来时间。
     */
    private boolean verifyTimestamp(String timestampStr) {
        if (!StringUtils.hasText(timestampStr)) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            // 只接受容忍范围内的过去时间戳，拒绝未来时间戳。
            return diff >= 0 && diff <= timestampToleranceSeconds() * 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 校验 nonce 不在黑名单中。
     */
    private boolean verifyNonce(String nonce) {
        if (!StringUtils.hasText(nonce)) {
            return false;
        }
        try {
            return !kvStore.exists("nonce:" + nonce);
        } catch (Exception e) {
            log.error("Failed to check nonce blacklist, rejecting request for security", e);
            return false; // 失败关闭：KV 不可用时拒绝请求。
        }
    }

    /**
     * 校验 HMAC 签名。
     */
    private boolean verifySignature(HttpServletRequest request, String timestamp, String nonce,
                                   String secretVersion, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            // 按参数名排序查询参数，对齐拦截器中的 buildQueryString()。
            String queryString = buildSortedQueryString(request.getQueryString());

            // 构造签名载荷：timestamp:nonce:method:path:query。
            String payload = timestamp + ":" + nonce + ":" + method + ":" + path + ":" + queryString;
            String expectedSignature = hmacSha256(payload, sharedSecret());

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * 基于原始 query string 构造排序后的查询字符串。
     * 该逻辑需要与 Feign 拦截器的 buildQueryString() 保持一致，确保签名计算稳定。
     */
    private String buildSortedQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        // 将 query string 拆成键值对，按 key 排序后重新拼装。
        return java.util.Arrays.stream(queryString.split("&"))
                .map(param -> {
                    int idx = param.indexOf('=');
                    if (idx > 0) {
                        return param.substring(0, idx) + "=" + param.substring(idx + 1);
                    }
                    return param;
                })
                .sorted()
                .collect(Collectors.joining("&"));
    }

    /**
     * 计算 HMAC-SHA256。
     */
    private String hmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hmacBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 返回 403 Forbidden 响应。
     */
    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }

    private String sharedSecret() {
        return properties.getInner().getSecret();
    }

    private long timestampToleranceSeconds() {
        return properties.getInner().getTimestampToleranceSeconds();
    }

    private long nonceTtlSeconds() {
        return properties.getInner().getNonceTtlSeconds();
    }
}
