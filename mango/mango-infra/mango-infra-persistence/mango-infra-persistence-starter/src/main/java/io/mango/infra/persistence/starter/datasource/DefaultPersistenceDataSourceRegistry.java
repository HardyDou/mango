package io.mango.infra.persistence.starter.datasource;

import io.mango.infra.persistence.api.datasource.PersistenceDataSourceDefinition;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认 Mango 数据源注册表。
 */
public class DefaultPersistenceDataSourceRegistry implements PersistenceDataSourceRegistry {

    private final String primaryName;

    private final Map<String, DataSource> dataSources;

    public DefaultPersistenceDataSourceRegistry(String primaryName, Map<String, DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Mango persistence datasources must not be empty");
        }
        if (!dataSources.containsKey(primaryName)) {
            throw new IllegalArgumentException("Primary datasource does not exist: " + primaryName);
        }
        this.primaryName = primaryName;
        this.dataSources = Collections.unmodifiableMap(new LinkedHashMap<>(dataSources));
    }

    @Override
    public String primaryName() {
        return primaryName;
    }

    @Override
    public Optional<DataSource> find(String name) {
        return Optional.ofNullable(dataSources.get(name));
    }

    @Override
    public DataSource get(String name) {
        DataSource dataSource = dataSources.get(name);
        if (dataSource == null) {
            throw new IllegalArgumentException("Mango datasource does not exist: " + name);
        }
        return dataSource;
    }

    @Override
    public Set<String> names() {
        return dataSources.keySet();
    }

    @Override
    public Set<PersistenceDataSourceDefinition> definitions() {
        return dataSources.keySet().stream()
                .map(name -> new PersistenceDataSourceDefinition(name, name.equals(primaryName)))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<Object, Object> targetDataSources() {
        return dataSources.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
