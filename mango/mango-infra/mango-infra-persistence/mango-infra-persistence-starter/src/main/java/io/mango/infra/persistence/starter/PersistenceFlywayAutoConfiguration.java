package io.mango.infra.persistence.starter;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mango Flyway 自动配置。
 * <p>
 * 负责按模块加载数据库迁移脚本，支持通过配置开启或关闭指定模块的迁移。
 * 迁移脚本目录约定为 {@code classpath:db/migration/{module}/V*.sql}。
 * <p>
 * 配置示例：
 * <pre>
 * mango:
 *   persistence:
 *     flyway:
 *       enabled: true                     # 全局开关
 *       modules:
 *         user:
 *           enabled: true                 # 模块开关，默认开启
 *           baseline-on-migrate: false    # 是否从已有库基线启动，默认关闭
 * </pre>
 * <p>
 * 使用本配置时，应由 Mango 管理 Flyway 迁移。
 *
 * @see PersistenceFlywayProperties
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(PersistenceFlywayProperties.class)
public class PersistenceFlywayAutoConfiguration {

    private static final String MIGRATION_LOCATION_PREFIX = "classpath:db/migration/";

    @Bean
    @DependsOn("dataSource")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(@Autowired DataSource dataSource,
                         @Autowired PersistenceFlywayProperties properties) {
        if (!properties.isEnabled()) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .validateOnMigrate(false)
                    .baselineOnMigrate(false)
                    .load();
        }

        List<String> enabledLocations = new ArrayList<>();
        boolean baselineOnMigrate = false;

        for (Map.Entry<String, PersistenceFlywayProperties.ModuleConfig> entry : properties.getModules().entrySet()) {
            PersistenceFlywayProperties.ModuleConfig config = entry.getValue();
            if (config == null || config.isEnabled()) {
                enabledLocations.add(MIGRATION_LOCATION_PREFIX + entry.getKey());
                if (config != null && config.isBaselineOnMigrate()) {
                    baselineOnMigrate = true;
                }
            }
        }

        if (enabledLocations.isEmpty()) {
            enabledLocations.add(MIGRATION_LOCATION_PREFIX);
        }

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(enabledLocations.toArray(new String[0]))
                .baselineOnMigrate(baselineOnMigrate)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(name = "persistenceFlywayMigrationInitializer")
    public ApplicationRunner persistenceFlywayMigrationInitializer(@Autowired Flyway flyway,
                                                                   @Autowired PersistenceFlywayProperties properties) {
        if (!properties.isEnabled()) {
            return (args) -> {
            };
        }
        return (args) -> flyway.migrate();
    }
}
