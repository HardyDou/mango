package io.mango.auth.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.core.service.ExternalAuthProviderAdapter;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.common.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class DingTalkAuthProviderAdapter implements ExternalAuthProviderAdapter {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String USER_URL = "https://api.dingtalk.com/v1.0/contact/users/me";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DingTalkAuthProviderAdapter(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    DingTalkAuthProviderAdapter(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ExternalAuthProvider provider() {
        return ExternalAuthProvider.DINGTALK;
    }

    @Override
    public String buildAuthorizationUrl(IAuthProviderConfigService.ResolvedProviderConfig config,
                                        String redirectUri, String state) {
        return "https://login.dingtalk.com/oauth2/auth?redirect_uri=" + encode(redirectUri)
                + "&response_type=code&client_id=" + encode(config.clientId())
                + "&scope=openid&state=" + encode(state)
                + "&prompt=consent";
    }

    @Override
    public ExternalAuthIdentity exchange(IAuthProviderConfigService.ResolvedProviderConfig config, String code) {
        String tokenBody = writeTokenRequest(config, code);
        JsonNode tokenResponse = send(HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody, StandardCharsets.UTF_8))
                .build());
        String accessToken = text(tokenResponse, "accessToken");
        if (!StringUtils.hasText(accessToken)) {
            throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权未返回访问令牌");
        }
        JsonNode user = send(HttpRequest.newBuilder(URI.create(USER_URL))
                .timeout(TIMEOUT)
                .header("x-acs-dingtalk-access-token", accessToken)
                .GET().build());
        String externalId = firstText(text(user, "unionId"), text(user, "openId"));
        if (!StringUtils.hasText(externalId)) {
            throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权未返回用户标识");
        }
        return new ExternalAuthIdentity(firstText(config.providerTenantId(), config.clientId()), externalId,
                text(user, "nick"));
    }

    private String writeTokenRequest(IAuthProviderConfigService.ResolvedProviderConfig config, String code) {
        try {
            return objectMapper.writeValueAsString(new TokenRequest(config.clientId(), config.secret(), code,
                    "authorization_code"));
        } catch (IOException exception) {
            throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权请求无法生成", exception);
        }
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权请求失败");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权网络异常", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(AuthCode.EXTERNAL_AUTH_FAILED.getCode(), "钉钉授权请求被中断", exception);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record TokenRequest(String clientId, String clientSecret, String code, String grantType) {
    }
}
