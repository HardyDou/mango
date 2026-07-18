package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModuleRoleTest {

    @Test
    void syncStarterSharesItsDomainIdentityWithThePrimaryStarter() {
        assertThat(ModuleRole.fromArtifactId("mango-resource-sync-starter"))
                .isEqualTo(ModuleRole.STARTER);
        assertThat(ModuleRole.domainOf("mango-resource-sync-starter"))
                .isEqualTo("mango-resource");
    }
}
