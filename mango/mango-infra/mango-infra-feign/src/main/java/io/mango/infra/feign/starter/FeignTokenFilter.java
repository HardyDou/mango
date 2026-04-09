package io.mango.infra.feign.starter;

import io.mango.common.context.TokenContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * Servlet filter to extract JWT token from incoming requests and store in TokenContextHolder.
 * <p>
 * This filter runs early in the filter chain to capture the Authorization header
 * before any Feign calls are made.
 *
 * @author Mango
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 4)
public class FeignTokenFilter implements Filter {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 4;

    private static final Logger log = LoggerFactory.getLogger(FeignTokenFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (servletRequest instanceof HttpServletRequest httpRequest) {
                String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
                if (authHeader != null && !authHeader.isEmpty()) {
                    TokenContextHolder.setToken(authHeader);
                    log.debug("Captured JWT token from request");
                }
            }
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            TokenContextHolder.clear();
        }
    }
}
