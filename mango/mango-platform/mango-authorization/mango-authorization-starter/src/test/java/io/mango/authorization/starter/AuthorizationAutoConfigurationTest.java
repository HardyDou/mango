package io.mango.authorization.starter;

import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.org.api.SysOrgApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthorizationAutoConfigurationTest {

    @Test
    void authorizationDataScopeProvider_shouldCreateProviderFromAutoConfiguration() {
        AuthorizationAutoConfiguration autoConfiguration = new AuthorizationAutoConfiguration();
        DataScopeProvider provider = autoConfiguration.authorizationDataScopeProvider(
                mock(IRoleDataScopeService.class),
                mock(TenantMemberProvider.class),
                mock(ObjectProvider.class));

        assertThat(provider).isInstanceOf(AuthorizationDataScopeProvider.class);
    }
}
