package io.mango.auth.starter.web;

import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.starter.controller.AuthController;
import io.mango.common.result.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Duration;
import java.util.Set;

/**
 * 在统一响应写出阶段维护浏览器认证 Cookie，不污染 API Controller 签名。
 */
@ControllerAdvice(assignableTypes = AuthController.class)
public class AuthCookieResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String COOKIE_NAME = "MANGO_TOKEN";
    private static final Set<String> LOGIN_PATHS = Set.of(
            "/auth/login", "/auth/wecom/login", "/auth/password/change-required");

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof R<?> result) || !result.isSuccess()) {
            return body;
        }
        String path = request.getURI().getPath();
        if (LOGIN_PATHS.contains(path) && result.getData() instanceof LoginVO login
                && login.getAccessToken() != null && !login.getAccessToken().isBlank()) {
            addCookie(response, login.getAccessToken(), null);
        } else if ("/auth/logout".equals(path)) {
            addCookie(response, "", Duration.ZERO);
        }
        return body;
    }

    private void addCookie(ServerHttpResponse response, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax");
        if (maxAge != null) {
            builder.maxAge(maxAge);
        }
        response.getHeaders().add(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
