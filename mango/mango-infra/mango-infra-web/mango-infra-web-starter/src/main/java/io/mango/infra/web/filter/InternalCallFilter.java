package io.mango.infra.web.filter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.infra.web.support.InternalCallAttributes;
import io.mango.infra.web.starter.MangoWebProperties;
import io.mango.infra.web.util.InternalCallSignature;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.http.server.PathContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

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
public class InternalCallFilter implements Filter, Ordered {

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    private static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    private static final String NONCE_HEADER = "X-Internal-Nonce";
    private static final String SIGNATURE_HEADER = "X-Internal-Signature";
    private static final String NONCE_PREFIX = "nonce:";
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    private final IInternalPathProvider internalPathProvider;
    private final IKvStore kvStore;
    private final MangoWebProperties properties;

    /**
     * 从内部路径提供器加载的内部路径集合。
     */
    private volatile List<PathPattern> internalPathPatterns = List.of();

    /**
     * 标记内部路径是否已成功加载。
     */
    private volatile boolean pathsLoaded = false;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring singleton collaborators are injected")
    public InternalCallFilter(IInternalPathProvider internalPathProvider, IKvStore kvStore,
                              MangoWebProperties properties) {
        this.internalPathProvider = internalPathProvider;
        this.kvStore = kvStore;
        this.properties = properties;
    }

    /**
     * 应用就绪后加载内部路径，启动加载失败时进入安全拒绝模式。
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        try {
            loadInternalPaths();
            this.pathsLoaded = true;
            log.info("Internal paths loaded successfully: count={}", internalPathPatterns.size());
        } catch (RuntimeException e) {
            this.pathsLoaded = false;
            log.error("Failed to load internal paths at startup, rejecting all requests", e);
        }
    }

    /**
     * 定时刷新内部路径；刷新失败时保留上一次可用结果。
     */
    @Scheduled(
            fixedRateString = "${mango.web.inner.path-refresh-interval-seconds:300}000",
            initialDelayString = "${mango.web.inner.path-refresh-interval-seconds:300}000")
    public void refreshInternalPaths() {
        try {
            loadInternalPaths();
            this.pathsLoaded = true;
            log.info("Internal paths refreshed: count={}", internalPathPatterns.size());
        } catch (RuntimeException e) {
            log.warn("Failed to refresh internal paths, keeping last known paths", e);
            // 保留上一次已知路径，pathsLoaded 仍保持 true。
        }
    }

    /**
     * 从提供器加载内部路径。
     */
    private void loadInternalPaths() {
        List<String> paths = internalPathProvider.getInternalPaths();
        if (paths == null) {
            throw new IllegalStateException("Internal path provider returned null");
        }
        this.internalPathPatterns = paths.stream()
                .map(PATH_PATTERN_PARSER::parse)
                .toList();
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

        // 安全拒绝：无法识别内部路径时拒绝所有请求，避免内部接口被误放行。
        if (!pathsLoaded) {
            sendForbidden(response, "Internal paths not loaded");
            return;
        }

        String path = request.getRequestURI();

        // 非内部路径直接放行。
        if (!isInternalPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String requestRejection = requestRejection(request);
        if (requestRejection != null) {
            sendForbidden(response, requestRejection);
            return;
        }

        String securityRejection = securityRejection(request);
        if (securityRejection != null) {
            sendForbidden(response, securityRejection);
            return;
        }

        request.setAttribute(InternalCallAttributes.VERIFIED, Boolean.TRUE);
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private String requestRejection(HttpServletRequest request) {
        if (!"true".equals(request.getHeader(INTERNAL_CALL_HEADER))) {
            return "Missing X-Internal-Call header";
        }
        if (!StringUtils.hasText(sharedSecret())) {
            log.error("No internal call secret configured, rejecting internal request");
            return "Internal call secret is not configured";
        }
        if (!verifyTimestamp(request.getHeader(TIMESTAMP_HEADER))) {
            return "Invalid or expired timestamp";
        }
        if (!StringUtils.hasText(request.getHeader(NONCE_HEADER))) {
            return "Nonce already used";
        }
        return null;
    }

    private String securityRejection(HttpServletRequest request) {
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (!verifySignature(request, timestamp, nonce, signature)) {
            return "Invalid signature";
        }
        if (!claimNonce(nonce)) {
            return "Nonce already used";
        }
        return null;
    }

    /**
     * 判断路径是否为内部路径。
     */
    private boolean isInternalPath(String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        for (PathPattern pattern : internalPathPatterns) {
            if (pattern.matches(pathContainer)) {
                return true;
            }
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
            return diff >= 0 && diff <= timestampToleranceSeconds() * MILLIS_PER_SECOND;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean claimNonce(String nonce) {
        try {
            return kvStore.setIfAbsent(NONCE_PREFIX + nonce, "1", nonceTtlSeconds());
        } catch (RuntimeException e) {
            log.error("Failed to claim nonce, rejecting request for security", e);
            return false;
        }
    }

    /**
     * 校验 HMAC 签名。
     */
    private boolean verifySignature(HttpServletRequest request, String timestamp, String nonce, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        try {
            String query = InternalCallSignature.canonicalizeRawQuery(request.getQueryString());
            String expected = InternalCallSignature.sign(timestamp, nonce, request.getMethod(),
                    request.getRequestURI(), query, sharedSecret());
            return InternalCallSignature.matches(signature, expected);
        } catch (RuntimeException e) {
            log.error("Signature verification failed", e);
            return false;
        }
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
