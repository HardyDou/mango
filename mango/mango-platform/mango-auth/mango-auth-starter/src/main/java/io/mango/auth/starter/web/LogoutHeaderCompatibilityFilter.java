package io.mango.auth.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 保留历史的“仅 Authorization 请求头退出”协议，并转换为当前统一命令体。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The ObjectMapper is an intentionally shared, thread-safe Spring collaborator")
public class LogoutHeaderCompatibilityFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (requiresCompatibilityBody(request)) {
            byte[] body = objectMapper.writeValueAsBytes(Map.of("token", request.getHeader("Authorization")));
            filterChain.doFilter(new JsonBodyRequestWrapper(request, body), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresCompatibilityBody(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (!"/auth/logout".equals(request.getRequestURI()) || request.getContentLengthLong() > 0) {
            return false;
        }
        String authorization = request.getHeader("Authorization");
        return authorization != null && !authorization.isBlank();
    }

    private static final class JsonBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        private JsonBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public String getContentType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // In-memory input is immediately available.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
