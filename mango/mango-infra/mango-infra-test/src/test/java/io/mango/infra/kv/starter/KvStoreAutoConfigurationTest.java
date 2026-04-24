package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.aspect.KvCapabilityAspect;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for KvStoreAutoConfiguration.
 * Tests @ConditionalOnProperty injection for all 6 configuration scenarios.
 */
class KvStoreAutoConfigurationTest {

    // ==================== Explicit Type: redis ====================

    @Test
    void typeRedis_withRedissonBean_injectsRedisKvStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)  // placeholder bean
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=redis")
                .run(context -> {
                    assertThat(context).hasBean("redisKvStore");
                });
    }

    @Test
    void typeRedis_withoutRedissonBean_doesNotInject() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=redis")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("redisKvStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: jdbc ====================

    @Test
    void typeJdbc_withJdbcTemplate_injectsJdbcKvStore() {
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)  // placeholder bean
                .withBean(JdbcTemplate.class, () -> null)
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=jdbc")
                .run(context -> {
                    assertThat(context).hasBean("jdbcKvStore");
                });
    }

    @Test
    void typeJdbc_withoutDataSource_doesNotInject() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=jdbc")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("jdbcKvStore");
                    assertThat(context).doesNotHaveBean(IKvStore.class);
                });
    }

    // ==================== Explicit Type: memory ====================

    @Test
    void typeMemory_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=memory")
                .run(context -> {
                    assertThat(context).hasBean("memoryKvStore");
                });
    }

    @Test
    void capabilityEnabled_registersKvCapabilityAspectFromStarter() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class, KvCapabilityAutoConfiguration.class))
                .withPropertyValues(
                        "mango.kv.store.type=memory",
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.cache=true",
                        "mango.kv.capability.serializer=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(KvCapabilityAspect.class);
                });
    }

    // ==================== Auto-detect: type=auto ====================

    @Test
    void typeAuto_withRedissonBean_injectsRedisKvStore() {
        new ApplicationContextRunner()
                .withBean(RedissonClient.class, () -> null)
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoRedisKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisKvStore.class);
                });
    }

    @Test
    void typeAuto_withDataSourceOnly_noRedis_injectsMemoryKvStore() {
        // auto 模式只在存在 RedissonClient 时选择 Redis；不会因为存在 JdbcTemplate 自动切换到 JDBC
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> null)
                .withBean(JdbcTemplate.class, () -> null)
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=auto")
                .run(context -> {
                    assertThat(context).hasBean("autoMemoryKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    @Test
    void typeAuto_noExternalDeps_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                // No RedissonClient, no DataSource
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=auto")
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
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                // No mango.kv.store.type property at all
                .run(context -> {
                    assertThat(context).hasBean("autoRedisKvStore");
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(RedisKvStore.class);
                });
    }

    @Test
    void typeNotConfigured_noDeps_injectsMemoryKvStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
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
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=redis")
                .run(context -> {
                    // Should NOT create a new redisKvStore bean
                    assertThat(context).doesNotHaveBean("redisKvStore");
                    // Original bean should still be there
                    assertThat(context.getBean(IKvStore.class)).isInstanceOf(MemoryKvStore.class);
                });
    }

    // ==================== KvStoreProperties test ====================

    @Test
    void kvStoreProperties_defaultType_isNullAndAutoSelectionIsDefaultedInConditions() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .run(context -> {
                    // @EnableConfigurationProperties creates a bean with property path name
                    KvStoreProperties props = context.getBean("mango.kv-io.mango.infra.kv.starter.KvStoreProperties", KvStoreProperties.class);
                    assertThat(props.getStore().getType()).isNull();
                });
    }

    @Test
    void kvStoreProperties_customStoreType_loadsCorrectly() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class))
                .withPropertyValues("mango.kv.store.type=memory")
                .run(context -> {
                    KvStoreProperties props = context.getBean("mango.kv-io.mango.infra.kv.starter.KvStoreProperties", KvStoreProperties.class);
                    assertThat(props.getStore().getType()).isEqualTo("memory");
                });
    }
}
