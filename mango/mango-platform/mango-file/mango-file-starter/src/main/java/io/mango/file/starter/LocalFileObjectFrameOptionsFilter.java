package io.mango.file.starter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 允许本地磁盘对象被同源后台页面内嵌预览。
 */
public class LocalFileObjectFrameOptionsFilter extends OncePerRequestFilter {

    private static final String FRAME_OPTIONS = "X-Frame-Options";
    private static final String SAMEORIGIN = "SAMEORIGIN";
    private static final String LOCAL_OBJECT_PREFIX = "/file/local-objects/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean localObjectPath = isLocalObjectPath(request.getRequestURI(), request.getContextPath());
        HttpServletResponse actualResponse = localObjectPath ? new SameOriginFrameOptionsResponse(response) : response;
        if (localObjectPath) {
            actualResponse.setHeader(FRAME_OPTIONS, SAMEORIGIN);
        }
        filterChain.doFilter(request, actualResponse);
        if (localObjectPath) {
            actualResponse.setHeader(FRAME_OPTIONS, SAMEORIGIN);
        }
    }

    private static boolean isLocalObjectPath(String requestUri, String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        return path.startsWith(LOCAL_OBJECT_PREFIX);
    }

    private static final class SameOriginFrameOptionsResponse extends HttpServletResponseWrapper {

        private SameOriginFrameOptionsResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, frameOptionsValue(name, value));
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, frameOptionsValue(name, value));
        }

        private static String frameOptionsValue(String name, String value) {
            if (FRAME_OPTIONS.equalsIgnoreCase(name)) {
                return SAMEORIGIN;
            }
            return value;
        }
    }
}
