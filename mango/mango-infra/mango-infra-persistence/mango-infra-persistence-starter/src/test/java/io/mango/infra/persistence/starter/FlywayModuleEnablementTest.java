package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayModuleEnablementTest {

    @Test
    void treatsUnknownModuleAsEnabled() {
        FlywayModuleEnablement enablement = new FlywayModuleEnablement(new PersistenceFlywayProperties());

        assertThat(enablement.isEnabled("integration")).isTrue();
        assertThat(enablement.isEnabled(null)).isTrue();
        assertThat(enablement.skipReason("integration")).isEmpty();
    }

    @Test
    void reportsDisabledModuleWithConfiguredReason() {
        PersistenceFlywayProperties properties = new PersistenceFlywayProperties();
        PersistenceFlywayProperties.ModuleConfig module = new PersistenceFlywayProperties.ModuleConfig();
        module.setEnabled(false);
        module.setSkipReason("集成模块未启用");
        properties.getModules().put("integration", module);
        FlywayModuleEnablement enablement = new FlywayModuleEnablement(properties);

        assertThat(enablement.isEnabled("integration")).isFalse();
        assertThat(enablement.skipReason("integration")).contains("集成模块未启用");
        assertThat(enablement.isEnabled("workflow")).isTrue();
    }

    @Test
    void treatsAllModulesAsDisabledWhenFlywayGloballyDisabled() {
        PersistenceFlywayProperties properties = new PersistenceFlywayProperties();
        properties.setEnabled(false);
        FlywayModuleEnablement enablement = new FlywayModuleEnablement(properties);

        assertThat(enablement.isEnabled("integration")).isFalse();
    }
}
