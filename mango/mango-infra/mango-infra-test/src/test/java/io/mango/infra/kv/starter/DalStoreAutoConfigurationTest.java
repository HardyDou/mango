package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.JdbcKvStore;
import io.mango.infra.kv.core.MemoryKvStore;
import io.mango.infra.kv.core.RedisKvStore;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DalStoreAutoConfiguration.
 * Tests @ConditionalOnProperty injection for all 6 configuration scenarios.
 */
class DalStoreAutoConfigurationTest {

    // ==================== Explicit Type: redis ====================

    @Test
    void typeRedis_withRedissonBean_injectsRedisKvStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)  // placeholder bean
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=redis")
                .run(context -> {
                    assertThat(context).hasBean("redisKvStore");
                });
    }

    @Test
    void typeRedis_withoutRedissonBean_doesNotInject() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=redis")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("redisKvStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: db ====================

    @Test
    void typeDb_withDataSourceAndRedissonBean_injectsJdbcKvStore() {
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)  // placeholder bean
                .withBean(RedissonClient.class, () -> null)
                .withBean(JdbcTemplate.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=db")
                .run(context -> {
                    assertThat(context).hasBean("dbKvStore");
                });
    }

    @Test
    void typeDb_withoutDataSource_doesNotInject() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=db")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("dbKvStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: memory ====================

    @Test
    void typeMemory_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=memory")
                .run(context -> {
                    assertThat(context).hasBean("memoryKvStore");
                });
    }

    // ==================== Auto-detect: type=auto ====================

    @Test
    void typeAuto_withRedissonBean_injectsRedisKvStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoRedisKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisKvStore.class);
                });
    }

    @Test
    void typeAuto_withDataSourceOnly_noRedis_injectsMemoryKvStore() {
        // When no RedissonClient, even with DataSource, auto-detect falls through to MemoryKvStore
        // because JdbcKvStore requires RedissonClient for ID generation
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)
                .withBean(JdbcTemplate.class, () -> null)
                // No RedissonClient bean
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    @Test
    void typeAuto_noExternalDeps_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                // No RedissonClient, no DataSource
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    // ==================== Auto-detect: type not configured (matchIfMissing) ====================

    @Test
    void typeNotConfigured_withRedissonBean_injectsRedisKvStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                // No mango.dal.type property at all
                .run(context -> {
                    assertThat(context).hasBean("autoRedisKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisKvStore.class);
                });
    }

    @Test
    void typeNotConfigured_noDeps_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                // No property, no external beans
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    // ==================== @ConditionalOnMissingBean behavior ====================

    @Test
    void ikvStoreAlreadyExists_doesNotInject() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withBean(IKvStore.class, () -> new MemoryKvStore())  // pre-existing bean factory
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=redis")
                .run(context -> {
                    // Should NOT create a new redisKvStore bean
                    assertThat(context).doesNotHaveBean("redisKvStore");
                    // Original bean should still be there
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    // ==================== DalStoreProperties test ====================

    @Test
    void dalStoreProperties_defaultType_isAuto() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .run(context -> {
                    // @EnableConfigurationProperties creates a bean with property path name
                    DalStoreProperties props = context.getBean("mango.dal-io.mango.infra.kv.starter.DalStoreProperties", DalStoreProperties.class);
                    assertThat(props.getType()).isEqualTo("auto");
                });
    }

    @Test
    void dalStoreProperties_customType_loadsCorrectly() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.type=memory")
                .run(context -> {
                    DalStoreProperties props = context.getBean("mango.dal-io.mango.infra.kv.starter.DalStoreProperties", DalStoreProperties.class);
                    assertThat(props.getType()).isEqualTo("memory");
                });
    }
}
