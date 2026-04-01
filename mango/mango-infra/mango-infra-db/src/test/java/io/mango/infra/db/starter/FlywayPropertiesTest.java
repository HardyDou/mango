package io.mango.infra.db.starter;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayPropertiesTest {

    @Test
    void enabled_shouldDefaultToTrue() {
        FlywayProperties props = new FlywayProperties();
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void disabled_module_shouldNotBeEnabled() {
        FlywayProperties props = new FlywayProperties();
        props.setEnabled(true);

        FlywayProperties.ModuleConfig userConfig = new FlywayProperties.ModuleConfig();
        userConfig.setEnabled(false);
        props.getModules().put("user", userConfig);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getModules().get("user").isEnabled()).isFalse();
    }

    @Test
    void baselineOnMigrate_shouldDefaultToFalse() {
        FlywayProperties props = new FlywayProperties();

        FlywayProperties.ModuleConfig i18nConfig = new FlywayProperties.ModuleConfig();
        i18nConfig.setEnabled(true);
        props.getModules().put("i18n", i18nConfig);

        assertThat(props.getModules().get("i18n").isBaselineOnMigrate()).isFalse();
    }

    @Test
    void allModulesEnabled_byDefault() {
        FlywayProperties props = new FlywayProperties();

        FlywayProperties.ModuleConfig userConfig = new FlywayProperties.ModuleConfig();
        userConfig.setEnabled(true);
        props.getModules().put("user", userConfig);

        FlywayProperties.ModuleConfig areaConfig = new FlywayProperties.ModuleConfig();
        areaConfig.setEnabled(true);
        props.getModules().put("area", areaConfig);

        assertThat(props.getModules()).hasSize(2);
        assertThat(props.getModules().get("user").isEnabled()).isTrue();
        assertThat(props.getModules().get("area").isEnabled()).isTrue();
    }

    @Test
    void setEnabled_shouldWork() {
        FlywayProperties props = new FlywayProperties();
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void moduleConfig_baselineOnMigrate_canBeSet() {
        FlywayProperties.ModuleConfig config = new FlywayProperties.ModuleConfig();
        config.setBaselineOnMigrate(true);
        assertThat(config.isBaselineOnMigrate()).isTrue();
    }
}
