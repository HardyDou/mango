package io.mango.infra.persistence.starter.datasource;

import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 基于配置的模块数据源解析器。
 */
public class DefaultPersistenceModuleDataSourceResolver implements PersistenceModuleDataSourceResolver {

    private final PersistenceDataSourceProperties properties;

    public DefaultPersistenceModuleDataSourceResolver(PersistenceDataSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> resolveDataSource(String moduleName) {
        if (!StringUtils.hasText(moduleName)) {
            return Optional.empty();
        }
        PersistenceDataSourceProperties.ModuleConfig config = properties.getModules().get(moduleName.trim());
        if (config == null || !StringUtils.hasText(config.getDatasource())) {
            return Optional.empty();
        }
        return Optional.of(config.getDatasource().trim());
    }
}
