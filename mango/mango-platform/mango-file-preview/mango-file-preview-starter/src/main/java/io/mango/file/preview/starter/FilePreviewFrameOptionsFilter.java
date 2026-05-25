package io.mango.file.preview.starter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 允许预览页使用同源 iframe 加载 PDF.js 等内置页面。
 */
public class FilePreviewFrameOptionsFilter extends OncePerRequestFilter {

    private static final String FRAME_OPTIONS = "X-Frame-Options";
    private static final String SAMEORIGIN = "SAMEORIGIN";
    private static final List<String> PREVIEW_PATH_PREFIXES = List.of(
            "/file-preview/",
            "/onlinePreview",
            "/picturesPreview",
            "/getCorsFile",
            "/_decompression",
            "/pdfjs/",
            "/js/",
            "/css/",
            "/images/",
            "/bootstrap/",
            "/highlight/",
            "/xlsx/",
            "/static/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean previewPath = isPreviewPath(request.getRequestURI(), request.getContextPath());
        HttpServletResponse actualResponse = previewPath ? new SameOriginFrameOptionsResponse(response) : response;
        if (previewPath) {
            actualResponse.setHeader(FRAME_OPTIONS, SAMEORIGIN);
        }
        filterChain.doFilter(request, actualResponse);
        if (previewPath) {
            actualResponse.setHeader(FRAME_OPTIONS, SAMEORIGIN);
        }
    }

    private static boolean isPreviewPath(String requestUri, String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        for (String prefix : PREVIEW_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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
