package io.mango.infra.persistence.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlywayPropertiesTest {

    @Test
    void enabled_shouldDefaultToTrue() {
        PersistenceFlywayProperties props = new PersistenceFlywayProperties();
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void disabled_module_shouldNotBeEnabled() {
        PersistenceFlywayProperties props = new PersistenceFlywayProperties();
        props.setEnabled(true);

        PersistenceFlywayProperties.ModuleConfig userConfig = new PersistenceFlywayProperties.ModuleConfig();
        userConfig.setEnabled(false);
        props.getModules().put("user", userConfig);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getModules().get("user").isEnabled()).isFalse();
    }

    @Test
    void baselineOnMigrate_shouldDefaultToTrue() {
        PersistenceFlywayProperties props = new PersistenceFlywayProperties();

        PersistenceFlywayProperties.ModuleConfig i18nConfig = new PersistenceFlywayProperties.ModuleConfig();
        i18nConfig.setEnabled(true);
        props.getModules().put("i18n", i18nConfig);

        assertThat(props.getModules().get("i18n").isBaselineOnMigrate()).isTrue();
    }

    @Test
    void allModulesEnabled_byDefault() {
        PersistenceFlywayProperties props = new PersistenceFlywayProperties();

        PersistenceFlywayProperties.ModuleConfig userConfig = new PersistenceFlywayProperties.ModuleConfig();
        userConfig.setEnabled(true);
        props.getModules().put("user", userConfig);

        PersistenceFlywayProperties.ModuleConfig areaConfig = new PersistenceFlywayProperties.ModuleConfig();
        areaConfig.setEnabled(true);
        props.getModules().put("area", areaConfig);

        assertThat(props.getModules()).hasSize(2);
        assertThat(props.getModules().get("user").isEnabled()).isTrue();
        assertThat(props.getModules().get("area").isEnabled()).isTrue();
    }

    @Test
    void setEnabled_shouldWork() {
        PersistenceFlywayProperties props = new PersistenceFlywayProperties();
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void moduleConfig_baselineOnMigrate_canBeSet() {
        PersistenceFlywayProperties.ModuleConfig config = new PersistenceFlywayProperties.ModuleConfig();
        config.setBaselineOnMigrate(false);
        assertThat(config.isBaselineOnMigrate()).isFalse();
    }

    @Test
    void moduleConfig_shouldAcceptHistoryTableAndDatasource() {
        PersistenceFlywayProperties.ModuleConfig config = new PersistenceFlywayProperties.ModuleConfig();
        config.setHistoryTable("flyway_schema_history_identity");
        config.getDatasource().setUrl("jdbc:mysql://127.0.0.1:3306/mango_identity");
        config.getDatasource().setUsername("root");
        config.getDatasource().setPassword("secret");
        config.getDatasource().setDriverClassName("com.mysql.cj.jdbc.Driver");

        assertThat(config.getHistoryTable()).isEqualTo("flyway_schema_history_identity");
        assertThat(config.getDatasource().getUrl()).isEqualTo("jdbc:mysql://127.0.0.1:3306/mango_identity");
        assertThat(config.getDatasource().getUsername()).isEqualTo("root");
        assertThat(config.getDatasource().getPassword()).isEqualTo("secret");
        assertThat(config.getDatasource().getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    }
}
