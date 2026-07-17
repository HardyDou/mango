package io.mango.link.starter.endpoint;

import io.mango.common.result.Require;
import io.mango.link.api.enums.LinkCode;
import io.mango.link.core.service.ILinkOpenService;
import io.mango.link.core.service.LinkJumpContext;
import io.mango.link.core.service.LinkRedirectContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 网址跳转原始 HTTP 协议入口，返回标准 302 响应。 */
@Component
@RequiredArgsConstructor
public class LinkRedirectEndpoint {

    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "url", "uid", "source", "token", "accessToken", "password", "secret");

    private final ILinkOpenService linkOpenService;

    public ServerResponse redirect(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        String rawId = serverRequest.param("id").orElse(null);
        Require.notBlank(rawId, LinkCode.LINK_BUSINESS_ERROR, "网址 ID 不能为空");
        Require.isTrue(rawId.matches("\\d+"), LinkCode.LINK_BUSINESS_ERROR, "网址 ID 格式非法");
        LinkRedirectContext redirectContext = LinkRedirectContext.builder()
                .id(Long.valueOf(rawId))
                .source(serverRequest.param("source").orElse(null))
                .clientIp(clientIp(request))
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .referer(request.getHeader(HttpHeaders.REFERER))
                .build();
        return found(linkOpenService.resolveRedirectUrl(redirectContext));
    }

    public ServerResponse jump(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        LinkJumpContext jumpContext = LinkJumpContext.builder()
                .url(serverRequest.param("url").orElse(null))
                .visitorId(serverRequest.param("uid").orElse(null))
                .source(serverRequest.param("source").orElse(null))
                .extraParams(extraParams(request))
                .clientIp(clientIp(request))
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .referer(request.getHeader(HttpHeaders.REFERER))
                .build();
        return found(linkOpenService.resolveJumpUrl(jumpContext));
    }

    private ServerResponse found(String targetUrl) {
        return ServerResponse.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(targetUrl).toASCIIString())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extraParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .filter(entry -> !SENSITIVE_PARAMS.contains(entry.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}
