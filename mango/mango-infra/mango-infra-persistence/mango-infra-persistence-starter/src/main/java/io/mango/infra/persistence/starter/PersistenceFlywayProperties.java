package io.mango.infra.persistence.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flyway 迁移配置。
 * <p>
 * 配置前缀：{@code mango.persistence.flyway}
 * <p>
 * 示例：
 * <pre>
 * mango:
 *   persistence:
 *     flyway:
 *       enabled: true
 *       modules:
 *         user:
 *           enabled: true
 *           baseline-on-migrate: false
 *         area:
 *           enabled: false
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mango.persistence.flyway")
public class PersistenceFlywayProperties {

    /**
     * 全局迁移开关，默认开启。
     */
    private boolean enabled = true;

    /**
     * 模块级迁移配置。
     * Key 为模块名称，例如 user、area、org。
     */
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    @Data
    public static class ModuleConfig {
        /**
         * 是否启用当前模块迁移，默认开启。
         */
        private boolean enabled = true;

        /**
         * 是否对当前模块启用基线迁移。
         * 适用于数据库已有表结构、Flyway 需要从指定基线开始接管的场景。
         */
        private boolean baselineOnMigrate = true;

        /**
         * 是否允许当前模块按非顺序版本补跑迁移。
         * 默认关闭；仅用于明确的历史库兼容或升级补偿场景。
         */
        private boolean outOfOrder = false;

        /**
         * 当前模块 Flyway history table。
         * 未配置时使用 flyway_schema_history_{module}。
         */
        private String historyTable;

        /**
         * 当前模块独立迁移数据源。
         * 未配置时使用应用主数据源。
         */
        private DataSourceConfig datasource = new DataSourceConfig();
    }

    @Data
    public static class DataSourceConfig {
        /**
         * JDBC URL。配置后当前模块迁移使用独立数据库。
         */
        private String url;

        /**
         * JDBC 驱动类名。
         */
        private String driverClassName;

        /**
         * 数据库用户名。
         */
        private String username;

        /**
         * 数据库密码。
         */
        private String password;
    }
}
