package io.mango.infra.persistence.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 持久化基础设施自动配置。
 * <p>
 * 统一承载关系型数据库相关基础能力，包括数据源、事务、数据库迁移、
 * MyBatis-Plus 插件和 Repository 契约等。业务模块只依赖本 starter，
 * 不直接感知底层持久化组件组合。
 */
@AutoConfiguration
@EnableTransactionManagement
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceAutoConfiguration {
}
