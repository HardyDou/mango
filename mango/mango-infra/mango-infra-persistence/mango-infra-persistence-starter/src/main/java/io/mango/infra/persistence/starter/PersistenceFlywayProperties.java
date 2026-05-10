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
        private boolean baselineOnMigrate = false;
    }
}
