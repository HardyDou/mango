package io.mango.auth.core.service;

import io.mango.auth.api.enums.ExternalAuthProvider;

public interface ExternalAuthProviderAdapter {

    ExternalAuthProvider provider();

    String buildAuthorizationUrl(IAuthProviderConfigService.ResolvedProviderConfig config,
                                 String redirectUri, String state);

    ExternalAuthIdentity exchange(IAuthProviderConfigService.ResolvedProviderConfig config, String code);

    record ExternalAuthIdentity(String providerTenantId, String externalUserId, String displayName) {
    }
}
