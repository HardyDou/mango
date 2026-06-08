package io.mango.file.core.service.impl;

import io.mango.file.core.config.FileProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 文件运行时访问地址组装器。
 */
@Component
@RequiredArgsConstructor
public class FileAccessUrlAssembler {

    private static final String FILE_DOWNLOAD_PATH = "/file/files/download";

    private final FileProperties properties;

    public String downloadUrl(Long fileId) {
        String externalBaseUrl = externalBaseUrl();
        if (StringUtils.hasText(externalBaseUrl)) {
            return UriComponentsBuilder.fromUriString(externalBaseUrl)
                    .path(FILE_DOWNLOAD_PATH)
                    .queryParam("id", fileId)
                    .build()
                    .toUriString();
        }
        return FILE_DOWNLOAD_PATH + "?id=" + fileId;
    }

    public String externalize(String url) {
        if (!StringUtils.hasText(url) || isAbsoluteUrl(url)) {
            return url;
        }
        String externalBaseUrl = externalBaseUrl();
        if (!StringUtils.hasText(externalBaseUrl)) {
            return url;
        }
        return joinBaseAndPath(externalBaseUrl, url.trim());
    }

    private String externalBaseUrl() {
        String publicBaseUrl = trimTrailingSlash(properties.getPublicBaseUrl());
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return externalBaseUrl(servletAttributes.getRequest());
        }
        return null;
    }

    private String externalBaseUrl(HttpServletRequest request) {
        String scheme = firstHeaderValue(request, "X-Forwarded-Proto", request.getScheme());
        String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host", null);
        String host = StringUtils.hasText(forwardedHost) ? forwardedHost : request.getServerName();
        int port = resolvePort(request, scheme, host);
        host = stripPort(host);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(host);
        if (port > 0 && !isDefaultPort(scheme, port)) {
            builder.port(port);
        }
        return builder.path(normalizePath(firstHeaderValue(request, "X-Forwarded-Prefix", request.getContextPath())))
                .build()
                .toUriString();
    }

    private String joinBaseAndPath(String baseUrl, String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String basePath = URI.create(baseUrl).getPath();
        String normalizedBasePath = normalizePath(basePath);
        if (StringUtils.hasText(normalizedBasePath)
                && normalizedPath.equals(normalizedBasePath)) {
            return baseUrl;
        }
        if (StringUtils.hasText(normalizedBasePath)
                && normalizedPath.startsWith(normalizedBasePath + "/")) {
            normalizedPath = normalizedPath.substring(normalizedBasePath.length());
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(normalizedPath)
                .build()
                .toUriString();
    }

    private boolean isAbsoluteUrl(String url) {
        String trimmed = url.trim();
        return trimmed.startsWith("//") || trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    }

    private String firstHeaderValue(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String first = value.split(",", 2)[0].trim();
        return StringUtils.hasText(first) ? first : fallback;
    }

    private int resolvePort(HttpServletRequest request, String scheme, String host) {
        String forwardedPort = firstHeaderValue(request, "X-Forwarded-Port", null);
        if (StringUtils.hasText(forwardedPort)) {
            try {
                return Integer.parseInt(forwardedPort);
            } catch (NumberFormatException ignored) {
                return request.getServerPort();
            }
        }
        int hostPort = portFromHost(host);
        return hostPort > 0 ? hostPort : request.getServerPort();
    }

    private int portFromHost(String host) {
        if (!StringUtils.hasText(host) || host.startsWith("[")) {
            return -1;
        }
        int index = host.lastIndexOf(':');
        if (index < 0 || index == host.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(host.substring(index + 1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String stripPort(String host) {
        if (!StringUtils.hasText(host) || host.startsWith("[")) {
            return host;
        }
        int index = host.lastIndexOf(':');
        if (index < 0) {
            return host;
        }
        return host.substring(0, index);
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
