package io.mango.dal.starter;

import io.mango.dal.api.IKvStore;
import io.mango.dal.core.DbXivStore;
import io.mango.dal.core.MemoryXivStore;
import io.mango.dal.core.RedisXivStore;
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
    void typeRedis_withRedissonBean_injectsRedisXivStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)  // placeholder bean
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=redis")
                .run(context -> {
                    assertThat(context).hasBean("redisXivStore");
                });
    }

    @Test
    void typeRedis_withoutRedissonBean_doesNotInject() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=redis")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("redisXivStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: db ====================

    @Test
    void typeDb_withDataSourceAndRedissonBean_injectsDbXivStore() {
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)  // placeholder bean
                .withBean(RedissonClient.class, () -> null)
                .withBean(JdbcTemplate.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=db")
                .run(context -> {
                    assertThat(context).hasBean("dbXivStore");
                });
    }

    @Test
    void typeDb_withoutDataSource_doesNotInject() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=db")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("dbXivStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: memory ====================

    @Test
    void typeMemory_injectsMemoryXivStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=memory")
                .run(context -> {
                    assertThat(context).hasBean("memoryXivStore");
                });
    }

    // ==================== Auto-detect: type=auto ====================

    @Test
    void typeAuto_withRedissonBean_injectsRedisXivStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoRedisXivStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisXivStore.class);
                });
    }

    @Test
    void typeAuto_withDataSourceOnly_noRedis_injectsMemoryXivStore() {
        // When no RedissonClient, even with DataSource, auto-detect falls through to MemoryXivStore
        // because DbXivStore requires RedissonClient for ID generation
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)
                .withBean(JdbcTemplate.class, () -> null)
                // No RedissonClient bean
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryXivStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryXivStore.class);
                });
    }

    @Test
    void typeAuto_noExternalDeps_injectsMemoryXivStore() {
        new ApplicationContextRunner()
                // No RedissonClient, no DataSource
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryXivStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryXivStore.class);
                });
    }

    // ==================== Auto-detect: type not configured (matchIfMissing) ====================

    @Test
    void typeNotConfigured_withRedissonBean_injectsRedisXivStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                // No mango.dal.kvstore.type property at all
                .run(context -> {
                    assertThat(context).hasBean("autoRedisXivStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisXivStore.class);
                });
    }

    @Test
    void typeNotConfigured_noDeps_injectsMemoryXivStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                // No property, no external beans
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryXivStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryXivStore.class);
                });
    }

    // ==================== @ConditionalOnMissingBean behavior ====================

    @Test
    void ikvStoreAlreadyExists_doesNotInject() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withBean(IKvStore.class, () -> new MemoryXivStore())  // pre-existing bean factory
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=redis")
                .run(context -> {
                    // Should NOT create a new redisXivStore bean
                    assertThat(context).doesNotHaveBean("redisXivStore");
                    // Original bean should still be there
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryXivStore.class);
                });
    }

    // ==================== DalStoreProperties test ====================

    @Test
    void dalStoreProperties_defaultType_isAuto() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .run(context -> {
                    // @EnableConfigurationProperties creates a bean with property path name
                    DalStoreProperties props = context.getBean("mango.dal.kvstore-io.mango.dal.starter.DalStoreProperties", DalStoreProperties.class);
                    assertThat(props.getType()).isEqualTo("auto");
                });
    }

    @Test
    void dalStoreProperties_customType_loadsCorrectly() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DalStoreAutoConfiguration.class))
                .withPropertyValues("mango.dal.kvstore.type=memory")
                .run(context -> {
                    DalStoreProperties props = context.getBean("mango.dal.kvstore-io.mango.dal.starter.DalStoreProperties", DalStoreProperties.class);
                    assertThat(props.getType()).isEqualTo("memory");
                });
    }
}
