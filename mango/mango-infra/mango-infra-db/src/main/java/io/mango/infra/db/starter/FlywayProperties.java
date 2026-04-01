package io.mango.infra.db.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyway Properties
 *
 * <p>Configuration prefix: {@code mango.flyway}
 * <p>Example:
 * <pre>
 * mango:
 *   flyway:
 *     enabled: true
 *     modules:
 *       user:
 *         enabled: true
 *         baseline-on-migrate: false
 *       area:
 *         enabled: false
 * </pre>
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.flyway")
public class FlywayProperties {

    /**
     * Global Flyway enable switch.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Per-module Flyway configuration.
     * Key = module name (e.g., user, area, org).
     * Default: all modules enabled.
     */
    private Map<String, ModuleConfig> modules = new HashMap<>();

    @Data
    public static class ModuleConfig {
        /**
         * Enable/disable this module's migration.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Run baseline on migrate for this module.
         * Use when the database already has tables and Flyway needs a starting point.
         * Default: false
         */
        private boolean baselineOnMigrate = false;
    }
}
