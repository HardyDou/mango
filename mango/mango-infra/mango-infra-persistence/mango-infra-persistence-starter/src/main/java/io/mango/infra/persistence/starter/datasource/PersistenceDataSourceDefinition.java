package io.mango.infra.persistence.starter.datasource;

/**
 * Mango 数据源定义。
 *
 * @param name 数据源名称。
 * @param primary 是否默认数据源。
 */
public record PersistenceDataSourceDefinition(String name, boolean primary) {
}
