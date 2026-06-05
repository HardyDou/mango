package io.mango.infra.persistence.starter.datasource;

import java.util.Optional;

/**
 * 模块到数据源解析器。
 */
public interface PersistenceModuleDataSourceResolver {

    /**
     * 解析模块使用的数据源。
     */
    Optional<String> resolveDataSource(String moduleName);
}
