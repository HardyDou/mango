package io.mango.infra.persistence.starter.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mango 多数据源配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.persistence")
public class PersistenceDataSourceProperties {

    /**
     * 数据源定义。Key 为数据源名称，例如 primary、job、powerjob。
     */
    private Map<String, DataSourceConfig> datasources = new LinkedHashMap<>();

    /**
     * 模块到数据源映射。Key 为模块名称，例如 mango-system、mango-job。
     */
    private Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    public String primaryName() {
        return datasources.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isPrimary())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("primary");
    }

    @Data
    public static class DataSourceConfig {

        /**
         * 是否为默认数据源。
         */
        private boolean primary;

        /**
         * JDBC URL。
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

    @Data
    public static class ModuleConfig {

        /**
         * 当前模块使用的数据源名称。
         */
        private String datasource;
    }
}
