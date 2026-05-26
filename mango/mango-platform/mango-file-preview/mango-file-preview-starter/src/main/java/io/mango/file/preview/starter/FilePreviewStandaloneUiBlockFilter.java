package io.mango.file.preview.starter;

import io.mango.file.preview.core.config.FilePreviewProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Blocks kkFileView standalone demo pages when the engine is embedded in Mango.
 */
public class FilePreviewStandaloneUiBlockFilter extends OncePerRequestFilter {

    private static final Set<String> BLOCKED_PATHS = Set.of(
            "/",
            "/index",
            "/index.html",
            "/record",
            "/sponsor",
            "/integrated",
            "/contact",
            "/fileUpload",
            "/createFolder",
            "/deleteFile",
            "/deleteFile/captcha",
            "/listFiles"
    );

    private final FilePreviewProperties properties;

    public FilePreviewStandaloneUiBlockFilter(FilePreviewProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (properties.isStandaloneUiEnabled() || !BLOCKED_PATHS.contains(normalizePath(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":404,\"message\":\"Not Found\"}");
    }

    private String normalizePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        if (path.isBlank()) {
            return "/";
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
