package io.mango.infra.persistence.starter.datasource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mango 数据源注册表。
 */
public interface PersistenceDataSourceRegistry {

    /**
     * 默认数据源名称。
     */
    String primaryName();

    /**
     * 查询数据源。
     */
    Optional<DataSource> find(String name);

    /**
     * 判断数据源是否存在。
     */
    default boolean contains(String name) {
        return find(name).isPresent();
    }

    /**
     * 获取数据源。
     */
    DataSource get(String name);

    /**
     * 数据源名称集合。
     */
    Set<String> names();

    /**
     * 数据源定义。
     */
    Set<PersistenceDataSourceDefinition> definitions();

    /**
     * 目标数据源映射。
     */
    Map<Object, Object> targetDataSources();
}
