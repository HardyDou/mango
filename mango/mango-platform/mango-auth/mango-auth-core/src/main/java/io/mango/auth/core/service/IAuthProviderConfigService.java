package io.mango.auth.core.service;

import io.mango.auth.api.command.SaveProviderConfigCommand;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.vo.AvailableProviderVO;
import io.mango.auth.api.vo.ProviderConfigVO;

import java.util.List;

public interface IAuthProviderConfigService {

    List<ProviderConfigVO> listCurrentTenant(String appCode);

    ProviderConfigVO save(SaveProviderConfigCommand command);

    List<AvailableProviderVO> listAvailable(ProviderScope scope);

    ResolvedProviderConfig requireAvailable(ProviderSelection selection);

    record ProviderScope(String tenantId, String appCode) {
    }

    record ProviderSelection(String tenantId, String appCode, ExternalAuthProvider provider) {
    }

    record ResolvedProviderConfig(Long id, String tenantId, String appCode, ExternalAuthProvider provider,
                                  String clientId, String providerTenantId, String agentId, String secret,
                                  List<String> redirectUris) {
        public ResolvedProviderConfig {
            redirectUris = List.copyOf(redirectUris);
        }

        @Override
        public List<String> redirectUris() {
            return List.copyOf(redirectUris);
        }
    }
}
