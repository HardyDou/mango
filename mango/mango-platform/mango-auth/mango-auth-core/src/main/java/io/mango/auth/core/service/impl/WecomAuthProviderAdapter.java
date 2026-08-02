package io.mango.auth.core.service.impl;

import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.core.service.ExternalAuthProviderAdapter;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.service.WecomLoginClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class WecomAuthProviderAdapter implements ExternalAuthProviderAdapter {

    private final WecomLoginClient client;

    @Override
    public ExternalAuthProvider provider() {
        return ExternalAuthProvider.WECOM;
    }

    @Override
    public String buildAuthorizationUrl(IAuthProviderConfigService.ResolvedProviderConfig config,
                                        String redirectUri, String state) {
        return "https://open.work.weixin.qq.com/wwopen/sso/qrConnect?appid=" + encode(config.providerTenantId())
                + "&agentid=" + encode(config.agentId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
    }

    @Override
    public ExternalAuthIdentity exchange(IAuthProviderConfigService.ResolvedProviderConfig config, String code) {
        String userId = client.getUserId(config.providerTenantId(), config.secret(), code);
        return new ExternalAuthIdentity(config.providerTenantId(), userId, null);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
