package io.mango.infra.db.starter;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flyway Auto Configuration for Mango scaffold.
 *
 * <p>Provides versioned database migration management with per-module enable/disable support.
 * Migration files follow the layout: {@code classpath:db/migration/{module}/V*.sql}
 *
 * <p>Configuration:
 * <pre>
 * mango:
 *   flyway:
 *     enabled: true                       # Global switch
 *     modules:
 *       user:
 *         enabled: true                   # Per-module switch (default: true)
 *         baseline-on-migrate: false      # Per-module baseline (default: false)
 * </pre>
 *
 * <p><strong>Usage note:</strong> This auto-configuration automatically disables
 * Spring Boot's default Flyway via {@code spring.flyway.enabled=false}.
 *
 * @author Mango
 * @see FlywayProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(FlywayProperties.class)
public class MangoFlywayAutoConfiguration {

    private static final String MIGRATION_LOCATION_PREFIX = "classpath:db/migration/";

    @Bean
    @DependsOn("dataSource")
    public Flyway flyway(@Autowired DataSource dataSource,
                         @Autowired FlywayProperties properties) {
        if (!properties.isEnabled()) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .validateOnMigrate(false)
                    .baselineOnMigrate(false)
                    .load();
        }

        List<String> enabledLocations = new ArrayList<>();
        boolean baselineOnMigrate = false;

        for (Map.Entry<String, FlywayProperties.ModuleConfig> entry : properties.getModules().entrySet()) {
            FlywayProperties.ModuleConfig config = entry.getValue();
            if (config == null || config.isEnabled()) {
                enabledLocations.add(MIGRATION_LOCATION_PREFIX + entry.getKey());
                if (config != null && config.isBaselineOnMigrate()) {
                    baselineOnMigrate = true;
                }
            }
        }

        if (enabledLocations.isEmpty()) {
            enabledLocations.add(MIGRATION_LOCATION_PREFIX);
        }

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(enabledLocations.toArray(new String[0]))
                .baselineOnMigrate(baselineOnMigrate)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }

    @Bean
    @Order(0)
    public ApplicationRunner flywayMigrationInitializer(@Autowired Flyway flyway,
                                                        @Autowired FlywayProperties properties) {
        if (!properties.isEnabled()) {
            return (args) -> {};
        }
        return (args) -> flyway.migrate();
    }
}
