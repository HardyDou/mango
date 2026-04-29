package io.mango.authorization.access.starter.web.filter;

import io.mango.authorization.access.core.AccessConstants;
import io.mango.authorization.access.core.auth.AccessResult;
import io.mango.authorization.access.core.auth.AccessService;
import io.mango.authorization.access.core.auth.AccessPrincipal;
import io.mango.infra.context.core.MangoContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

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

    private final AccessService gatewayAccessService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        AccessResult result = gatewayAccessService.check(path, request.getHeader(AccessConstants.TOKEN_HEADER));

        if (result.status() == AccessResult.Status.FORBIDDEN) {
            forbidden(response, result.message());
            return;
        }
        if (result.status() == AccessResult.Status.UNAUTHORIZED) {
            unauthorized(response, result.message());
            return;
        }
        if (result.principal() != null) {
            writePrincipal(request, result.principal());
        }
        chain.doFilter(request, response);
    }

    private void writePrincipal(HttpServletRequest request, AccessPrincipal principal) {
        request.setAttribute("userId", principal.userId());
        request.setAttribute("username", principal.username());
        request.setAttribute("tenantId", principal.tenantId());
        MangoContextHolder.update(current -> current.withSecurity(
                principal.userId(),
                principal.tenantId(),
                principal.username(),
                principal.realm(),
                principal.actorType(),
                principal.partyType(),
                principal.partyId(),
                principal.appCode()));
    }

    /**
     * 返回未授权响应
     */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }

    private void forbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }
}
