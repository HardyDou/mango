package io.mango.access.starter.web.filter;

import io.mango.access.core.AccessConstants;
import io.mango.access.api.vo.AccessPrincipalVO;
import io.mango.access.api.vo.AccessResultVO;
import io.mango.access.core.auth.AccessEvaluator;
import io.mango.authorization.api.ITokenProvider;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证过滤器（单体模式）。
 * <p>
 * 基于 Servlet Filter 实现，兼容 Spring MVC。
 *
 * @author Mango
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthFilter implements Filter {

    private final AccessEvaluator accessEvaluator;
    private final ITokenProvider tokenProvider;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        clearUntrustedSecurityContext();
        if (isRealtimeTicketPath(path) && hasText(request.getParameter("rtTicket"))) {
            chain.doFilter(request, response);
            return;
        }
        String credential = resolveTokenCredential(request);
        establishTrustedSecurityContext(credential);
        AccessResultVO result = accessEvaluator.check(
                request.getMethod(),
                path,
                credential,
                request.getRemoteAddr());

        if (result.status() == AccessResultVO.Status.FORBIDDEN) {
            clearUntrustedSecurityContext();
            forbidden(response, result.message());
            return;
        }
        if (result.status() == AccessResultVO.Status.UNAUTHORIZED) {
            clearUntrustedSecurityContext();
            unauthorized(response, result.message());
            return;
        }
        if (result.status() == AccessResultVO.Status.SERVICE_UNAVAILABLE) {
            clearUntrustedSecurityContext();
            serviceUnavailable(response, result.message());
            return;
        }
        if (result.principal() != null) {
            writePrincipal(request, result.principal());
        } else {
            clearUntrustedSecurityContext();
        }
        chain.doFilter(request, response);
    }

    private void establishTrustedSecurityContext(String credential) {
        if (!hasText(credential) || !credential.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return;
        }
        String token = credential.substring(ITokenProvider.BEARER_PREFIX.length());
        try {
            if (!tokenProvider.validateToken(token)
                    || !ITokenProvider.TOKEN_TYPE_ACCESS.equals(tokenProvider.getTokenType(token))) {
                return;
            }
            writeSecurityContext(new AccessPrincipalVO(
                    tokenProvider.getUserId(token),
                    parseLong(tokenProvider.getClaim(token, "memberId")),
                    tokenProvider.getUsername(token),
                    tokenProvider.getClaim(token, "tenantId"),
                    tokenProvider.getClaim(token, "realm"),
                    tokenProvider.getClaim(token, "actorType"),
                    tokenProvider.getClaim(token, "partyType"),
                    parseLong(tokenProvider.getClaim(token, "partyId")),
                    tokenProvider.getClaim(token, "appCode")));
        } catch (RuntimeException exception) {
            clearUntrustedSecurityContext();
            log.debug("Failed to establish trusted access context before evaluation", exception);
        }
    }

    private String resolveTokenCredential(HttpServletRequest request) {
        String authHeader = request.getHeader(AccessConstants.TOKEN_HEADER);
        if (authHeader != null && !authHeader.isBlank()) {
            return authHeader;
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            String trimmed = queryToken.trim();
            return trimmed.startsWith("Bearer ") ? trimmed : "Bearer " + trimmed;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("MANGO_TOKEN".equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value == null || value.isBlank()) {
                    return null;
                }
                String trimmed = value.trim();
                return trimmed.startsWith("Bearer ") ? trimmed : "Bearer " + trimmed;
            }
        }
        return null;
    }

    private boolean isRealtimeTicketPath(String path) {
        return path.startsWith("/realtime/transports/probe/");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void writePrincipal(HttpServletRequest request, AccessPrincipalVO principal) {
        request.setAttribute("userId", principal.userId());
        request.setAttribute("memberId", principal.memberId());
        request.setAttribute("username", principal.username());
        request.setAttribute("tenantId", principal.tenantId());
        writeSecurityContext(principal);
    }

    private void writeSecurityContext(AccessPrincipalVO principal) {
        MangoContextSnapshot current = MangoContextHolder.get();
        MangoContextHolder.set(new MangoContextSnapshot(
                current.requestId(), current.traceId(), principal.tenantId(),
                principal.userId(),
                principal.memberId(),
                principal.username(),
                principal.realm(),
                principal.actorType(),
                principal.partyType(),
                principal.partyId(),
                principal.appCode(), current.clientIp()));
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void clearUntrustedSecurityContext() {
        MangoContextSnapshot current = MangoContextHolder.get();
        MangoContextHolder.set(new MangoContextSnapshot(
                current.requestId(), current.traceId(), null, null, null, null,
                null, null, null, null, null, current.clientIp()));
    }

    /**
     * 返回未授权响应
     */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(errorBody(401, message));
    }

    private void forbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(errorBody(403, message));
    }

    private void serviceUnavailable(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(errorBody(503, message));
    }

    private String errorBody(int code, String message) {
        String safe = message == null ? "" : message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"code\":" + code + ",\"message\":\"" + safe + "\"}";
    }
}
