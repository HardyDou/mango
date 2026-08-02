package io.mango.auth.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.service.WecomLoginClient;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalAuthProviderAdapterTest {

    @Test
    void wecomBuildsQrAuthorizationAndReturnsClientIdentity() {
        WecomLoginClient client = mock(WecomLoginClient.class);
        when(client.getUserId("corp-id", "secret", "code")).thenReturn("external-user");
        WecomAuthProviderAdapter adapter = new WecomAuthProviderAdapter(client);
        IAuthProviderConfigService.ResolvedProviderConfig config = config(
                ExternalAuthProvider.WECOM, "corp-id", "corp-id", "1000003");

        String url = adapter.buildAuthorizationUrl(config, "https://admin.example.com/callback", "opaque-state");
        var identity = adapter.exchange(config, "code");

        assertThat(url).startsWith("https://open.work.weixin.qq.com/wwopen/sso/qrConnect?")
                .contains("appid=corp-id")
                .contains("agentid=1000003")
                .contains("redirect_uri=https%3A%2F%2Fadmin.example.com%2Fcallback")
                .contains("state=opaque-state")
                .doesNotContain("secret");
        assertThat(identity.providerTenantId()).isEqualTo("corp-id");
        assertThat(identity.externalUserId()).isEqualTo("external-user");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dingtalkParsesStructuredTokenAndUserResponses() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        HttpResponse<String> userResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"accessToken\":\"token-value\",\"expireIn\":7200}");
        when(userResponse.statusCode()).thenReturn(200);
        when(userResponse.body()).thenReturn(
                "{\"unionId\":\"union-id\",\"openId\":\"open-id\",\"nick\":\"测试用户\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse, userResponse);
        DingTalkAuthProviderAdapter adapter = new DingTalkAuthProviderAdapter(new ObjectMapper(), httpClient);
        IAuthProviderConfigService.ResolvedProviderConfig config = config(
                ExternalAuthProvider.DINGTALK, "client-id", "organization-id", null);

        String url = adapter.buildAuthorizationUrl(config, "https://admin.example.com/callback", "opaque-state");
        var identity = adapter.exchange(config, "authorization-code");

        assertThat(url).startsWith("https://login.dingtalk.com/oauth2/auth?")
                .contains("client_id=client-id")
                .contains("state=opaque-state")
                .doesNotContain("secret");
        assertThat(identity.providerTenantId()).isEqualTo("organization-id");
        assertThat(identity.externalUserId()).isEqualTo("union-id");
        assertThat(identity.displayName()).isEqualTo("测试用户");
    }

    private IAuthProviderConfigService.ResolvedProviderConfig config(
            ExternalAuthProvider provider, String clientId, String providerTenantId, String agentId) {
        return new IAuthProviderConfigService.ResolvedProviderConfig(
                1L, "1", "internal-admin", provider, clientId, providerTenantId, agentId, "secret",
                List.of("https://admin.example.com/callback"));
    }
}
