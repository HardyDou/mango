package io.mango.infra.persistence.starter.datasource;

import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 基于配置的模块数据源解析器。
 */
public class DefaultPersistenceModuleDataSourceResolver implements PersistenceModuleDataSourceResolver {

    private final PersistenceDataSourceProperties properties;

    private final PersistenceModuleDataSourceDefaults moduleDefaults;

    private final PersistenceDataSourceRegistry registry;

    public DefaultPersistenceModuleDataSourceResolver(PersistenceDataSourceProperties properties,
                                                      PersistenceModuleDataSourceDefaults moduleDefaults,
                                                      PersistenceDataSourceRegistry registry) {
        this.properties = properties;
        this.moduleDefaults = moduleDefaults;
        this.registry = registry;
    }

    @Override
    public Optional<String> resolveDataSource(String moduleName) {
        if (!StringUtils.hasText(moduleName)) {
            return Optional.empty();
        }
        String normalizedModuleName = moduleName.trim();
        PersistenceDataSourceProperties.ModuleConfig config = properties.getModules().get(normalizedModuleName);
        if (config == null || !StringUtils.hasText(config.getDatasource())) {
            return moduleDefaults.resolve(normalizedModuleName)
                    .filter(registry::contains)
                    .or(() -> Optional.of(registry.primaryName()));
        }
        return Optional.of(config.getDatasource().trim());
    }
}
