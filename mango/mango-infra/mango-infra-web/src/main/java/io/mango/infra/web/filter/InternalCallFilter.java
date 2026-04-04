package io.mango.infra.web.filter;

import io.mango.common.result.R;
import io.mango.kv.api.IKvStore;
import io.mango.permission.api.SysPublicPathApi;
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
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Internal call filter - blocks direct access to internal APIs
 *
 * <p>流量架构：
 * <ul>
 *   <li>外部请求 → 必须过Gateway</li>
 *   <li>微服务间调用 → Feign携带 X-Internal-Call + 签名Header</li>
 *   <li>直接访问微服务 → 本Filter拦截并拒绝</li>
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

    private final SysPublicPathApi sysPublicPathApi;
    private final IKvStore kvStore;

    /**
     * Internal paths loaded from database
     */
    private final Set<String> internalPaths = new CopyOnWriteArraySet<>();

    /**
     * Flag indicating if paths have been loaded successfully
     */
    private volatile boolean pathsLoaded = false;

    /**
     * Timestamp of last successful load
     */
    private volatile long lastKnownPathsLoadTime = 0;

    /**
     * Shared secret for HMAC signature verification
     */
    @org.springframework.beans.factory.annotation.Value("${mango.internal-call.secret:}")
    private String sharedSecret;

    /**
     * Timestamp tolerance in seconds (default 5 minutes)
     */
    @org.springframework.beans.factory.annotation.Value("${mango.internal-call.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds = 300;

    /**
     * Nonce blacklist TTL in seconds
     */
    @org.springframework.beans.factory.annotation.Value("${mango.internal-call.nonce-ttl-seconds:300}")
    private long nonceTtlSeconds = 300;

    /**
     * Load internal paths on application ready (fail-secure)
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
     * Refresh internal paths every 5 minutes (graceful degradation)
     */
    @Scheduled(fixedRateString = "${mango.internal-call.path-refresh-interval-seconds:300}000")
    public void refreshInternalPaths() {
        try {
            loadInternalPaths();
            this.pathsLoaded = true;
            this.lastKnownPathsLoadTime = System.currentTimeMillis();
            log.info("Internal paths refreshed: count={}", internalPaths.size());
        } catch (Exception e) {
            log.warn("Failed to refresh internal paths, keeping last known paths", e);
            // Keep last known paths, pathsLoaded stays true
        }
    }

    /**
     * Load internal paths from database
     */
    private void loadInternalPaths() {
        R<?> result = sysPublicPathApi.listInternalPaths();
        if (result != null && result.getData() != null) {
            @SuppressWarnings("unchecked")
            var paths = (java.util.List<String>) result.getData();
            this.internalPaths.clear();
            this.internalPaths.addAll(paths);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Skip if not an internal path
        if (!isInternalPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Fail-secure: if paths not loaded at startup, reject all
        if (!pathsLoaded) {
            sendForbidden(response, "Internal paths not loaded");
            return;
        }

        // Check for X-Internal-Call header
        String internalCall = request.getHeader("X-Internal-Call");
        if (!"true".equals(internalCall)) {
            sendForbidden(response, "Missing X-Internal-Call header");
            return;
        }

        // Verify signature
        if (!StringUtils.hasText(sharedSecret)) {
            // If no secret configured, skip signature verification (dev mode)
            log.warn("No internal call secret configured, skipping signature verification");
            chain.doFilter(request, response);
            return;
        }

        // Verify timestamp
        String timestampStr = request.getHeader("X-Internal-Timestamp");
        if (!verifyTimestamp(timestampStr)) {
            sendForbidden(response, "Invalid or expired timestamp");
            return;
        }

        // Verify nonce (anti-replay)
        String nonce = request.getHeader("X-Internal-Nonce");
        if (!verifyNonce(nonce)) {
            sendForbidden(response, "Nonce already used");
            return;
        }

        // Verify signature
        String signature = request.getHeader("X-Internal-Signature");
        String secretVersion = request.getHeader("X-Internal-Secret-Version");
        if (!verifySignature(request, timestampStr, nonce, secretVersion, signature)) {
            sendForbidden(response, "Invalid signature");
            return;
        }

        // Nonce passed verification, add to blacklist
        if (StringUtils.hasText(nonce)) {
            try {
                kvStore.put("nonce:" + nonce, "1", nonceTtlSeconds);
            } catch (Exception e) {
                log.warn("Failed to add nonce to blacklist", e);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if path is internal
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
     * Path matching with wildcard support
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
     * Verify timestamp is within tolerance (past only, no future timestamps)
     */
    private boolean verifyTimestamp(String timestampStr) {
        if (!StringUtils.hasText(timestampStr)) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            // Only accept past timestamps within tolerance (reject future timestamps)
            return diff >= 0 && diff <= timestampToleranceSeconds * 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verify nonce not in blacklist
     */
    private boolean verifyNonce(String nonce) {
        if (!StringUtils.hasText(nonce)) {
            return false;
        }
        try {
            return !kvStore.exists("nonce:" + nonce);
        } catch (Exception e) {
            log.error("Failed to check nonce blacklist, rejecting request for security", e);
            return false; // Fail closed - reject if KV store unavailable
        }
    }

    /**
     * Verify HMAC signature
     */
    private boolean verifySignature(HttpServletRequest request, String timestamp, String nonce,
                                   String secretVersion, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            // Sort query params alphabetically to match interceptor's buildQueryString()
            String queryString = buildSortedQueryString(request.getQueryString());

            // Build payload: timestamp:nonce:method:path:query
            String payload = timestamp + ":" + nonce + ":" + method + ":" + path + ":" + queryString;
            String expectedSignature = hmacSha256(payload, sharedSecret);

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Build sorted query string from raw query string.
     * Matches Feign interceptor's buildQueryString() for consistent signature calculation.
     */
    private String buildSortedQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        // Parse query string into key-value pairs, sort by key, rebuild
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
     * Calculate HMAC-SHA256
     */
    private String hmacSha256(String data, String secret) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
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
     * Send 403 Forbidden response
     */
    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }
}
