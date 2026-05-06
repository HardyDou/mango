package io.mango.infra.persistence.starter;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlywayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceFlywayAutoConfiguration.class));

    @Test
    void whenEnabled_shouldCreateFlywayBean() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.user.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx).hasSingleBean(org.springframework.boot.ApplicationRunner.class);
                });
    }

    @Test
    void disabled_module_shouldNotBeInFlywayLocations() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.user.enabled=true",
                        "mango.persistence.flyway.modules.i18n.enabled=false"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    Flyway flyway = ctx.getBean(Flyway.class);
                    ClassicConfiguration config = (ClassicConfiguration) flyway.getConfiguration();
                    List<String> locations = Arrays.stream(config.getLocations())
                            .map(Location::getPath)
                            .collect(Collectors.toList());

                    assertThat(locations).contains("db/migration/user");
                    assertThat(locations).doesNotContain("db/migration/i18n");
                });
    }

    @Test
    void whenNoModulesSpecified_shouldUseDefaultLocation() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    Flyway flyway = ctx.getBean(Flyway.class);
                    ClassicConfiguration config = (ClassicConfiguration) flyway.getConfiguration();
                    List<String> locations = Arrays.stream(config.getLocations())
                            .map(Location::getPath)
                            .collect(Collectors.toList());

                    assertThat(locations).contains("db/migration");
                });
    }

    @Test
    void multipleModulesEnabled_shouldIncludeAllLocations() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.user.enabled=true",
                        "mango.persistence.flyway.modules.area.enabled=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    Flyway flyway = ctx.getBean(Flyway.class);
                    ClassicConfiguration config = (ClassicConfiguration) flyway.getConfiguration();
                    List<String> locations = Arrays.stream(config.getLocations())
                            .map(Location::getPath)
                            .collect(Collectors.toList());

                    assertThat(locations).contains("db/migration/user");
                    assertThat(locations).contains("db/migration/area");
                });
    }

    @Test
    void baselineOnMigrate_shouldBeConfigurable() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.flyway.enabled=true",
                        "mango.persistence.flyway.modules.user.enabled=true",
                        "mango.persistence.flyway.modules.user.baseline-on-migrate=true"
                )
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    Flyway flyway = ctx.getBean(Flyway.class);
                    ClassicConfiguration config = (ClassicConfiguration) flyway.getConfiguration();
                    assertThat(config.isBaselineOnMigrate()).isTrue();
                });
    }

    @Test
    void applicationRunner_shouldBeCreated() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(org.springframework.boot.ApplicationRunner.class);
                });
    }

    @Test
    void customFlyway_shouldNotBeOverridden() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=true")
                .withUserConfiguration(H2DataSourceConfig.class, CustomFlywayConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx.getBean(Flyway.class)).isSameAs(ctx.getBean("customFlyway"));
                });
    }

    @Test
    void disabled_shouldCreateNonMigratingFlywayToBlockBootDefaultFlow() {
        contextRunner
                .withPropertyValues("mango.persistence.flyway.enabled=false")
                .withUserConfiguration(H2DataSourceConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Flyway.class);
                    assertThat(ctx).hasSingleBean(org.springframework.boot.ApplicationRunner.class);
                });
    }

    @Configuration
    static class H2DataSourceConfig {
        @Bean
        DataSource dataSource() throws Exception {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }

    @Configuration
    static class CustomFlywayConfig {
        @Bean
        Flyway customFlyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:custom/migration")
                    .load();
        }
    }
}
