package io.mango.infra.kv.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KV store and capability configuration properties.
 *
 * 配置结构：
 * <pre>
 * mango:
 *   kv:
 *     store:
 *       type: auto/redis/jdbc/memory
 *     provider:
 *       jdbc:
 *         tableName: infra_kv_entry
 *       memory:
 *         cleanupIntervalMinutes: 1
 *     key:
 *       prefix: mango:kv
 *       env: default
 *       appEnabled: false
 *     capability:
 *       enabled: false
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mango.kv")
public class KvStoreProperties {

    private static final int DEFAULT_REDIS_PORT = 6379;
    private static final int DEFAULT_REDIS_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_REDIS_POOL_SIZE = 8;
    private static final int DEFAULT_JDBC_DRUID_INITIAL_SIZE = 5;
    private static final int DEFAULT_JDBC_DRUID_MAX_ACTIVE = 20;
    private static final long DEFAULT_JDBC_DRUID_MAX_WAIT_MILLIS = 60000L;
    private static final long DEFAULT_JDBC_DRUID_MIN_EVICTABLE_IDLE_MILLIS = 300000L;
    private static final int DEFAULT_HIKARI_MAX_POOL_SIZE = 10;
    private static final long DEFAULT_HIKARI_CONNECTION_TIMEOUT_MILLIS = 30000L;
    private static final long DEFAULT_HIKARI_IDLE_TIMEOUT_MILLIS = 600000L;
    private static final long DEFAULT_HIKARI_MAX_LIFETIME_MILLIS = 1800000L;

    /**
     * Store selection configuration.
     */
    private Store store = new Store();

    /**
     * Provider-specific configurations
     */
    private Provider provider = new Provider();

    /**
     * Capability bean default assembly configuration.
     */
    private Capability capability = new Capability();

    /**
     * KV key namespace configuration.
     */
    private Key key = new Key();

    @Data
    public static class Store {
        /**
         * KV store type: auto / redis / jdbc / memory.
         */
        private String type;
    }

    @Data
    public static class Provider {

        private Redis redis = new Redis();

        private Jdbc jdbc = new Jdbc();

        private Memory memory = new Memory();
    }

    /**
     * RedisKvStore configuration.
     */
    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = DEFAULT_REDIS_PORT;
        private String password;
        private int database = 0;
        private int timeout = DEFAULT_REDIS_TIMEOUT_MILLIS;
        private Pool pool = new Pool();

        @Data
        public static class Pool {
            private int maxActive = DEFAULT_REDIS_POOL_SIZE;
            private int maxIdle = DEFAULT_REDIS_POOL_SIZE;
            private int minIdle = 0;
            private int maxWait = -1;
        }
    }

    /**
     * JdbcKvStore configuration.
     *
     * 数据库连接配置优先级：
     * - mango.kv.provider.jdbc.url        > spring.datasource.url
     * - mango.kv.provider.jdbc.username   > spring.datasource.username
     * - mango.kv.provider.jdbc.password   > spring.datasource.password
     * - mango.kv.provider.jdbc.driver     > spring.datasource.driver-class-name
     * - Druid: mango.kv.provider.jdbc.druid.* > spring.datasource.druid.*
     * - Hikari: mango.kv.provider.jdbc.hikari.* > spring.datasource.hikari.*
     */
    @Data
    public static class Jdbc {
        /** KV 存储表名，默认 infra_kv_entry */
        private String tableName = "infra_kv_entry";

        /** JDBC URL，fallback 到 spring.datasource.url */
        private String url;

        /** 用户名，fallback 到 spring.datasource.username */
        private String username;

        /** 密码，fallback 到 spring.datasource.password */
        private String password;

        /** 驱动类，fallback 到 spring.datasource.driver-class-name */
        private String driver;

        /** Druid 连接池配置，fallback 到 spring.datasource.druid.* */
        private Druid druid = new Druid();

        /** HikariCP 连接池配置，fallback 到 spring.datasource.hikari.* */
        private Hikari hikari = new Hikari();

        @Data
        public static class Druid {
            private int initialSize = DEFAULT_JDBC_DRUID_INITIAL_SIZE;
            private int maxActive = DEFAULT_JDBC_DRUID_MAX_ACTIVE;
            private int minIdle = DEFAULT_JDBC_DRUID_INITIAL_SIZE;
            private long maxWait = DEFAULT_JDBC_DRUID_MAX_WAIT_MILLIS;
            private long timeBetweenEvictionRunsMillis = DEFAULT_JDBC_DRUID_MAX_WAIT_MILLIS;
            private long minEvictableIdleTimeMillis = DEFAULT_JDBC_DRUID_MIN_EVICTABLE_IDLE_MILLIS;
            private String validationQuery = "SELECT 1";
            private boolean testWhileIdle = true;
            private boolean testOnBorrow = false;
            private boolean testOnReturn = false;
        }

        @Data
        public static class Hikari {
            private int maxPoolSize = DEFAULT_HIKARI_MAX_POOL_SIZE;
            private int minIdle = DEFAULT_JDBC_DRUID_INITIAL_SIZE;
            private long connectionTimeout = DEFAULT_HIKARI_CONNECTION_TIMEOUT_MILLIS;
            private long idleTimeout = DEFAULT_HIKARI_IDLE_TIMEOUT_MILLIS;
            private long maxLifetime = DEFAULT_HIKARI_MAX_LIFETIME_MILLIS;
        }
    }

    /**
     * MemoryKvStore configuration.
     */
    @Data
    public static class Memory {
        /**
         * 过期 key 清理任务间隔（分钟），默认 1
         */
        private int cleanupIntervalMinutes = 1;
    }

    /**
     * KV key namespace configuration.
     */
    @Data
    public static class Key {
        /**
         * Whether capability beans should prepend the Mango KV namespace.
         */
        private boolean enabled = true;

        /**
         * Stable namespace root. Final format:
         * {prefix}:{env}[:{app}]:{capability}:{biz-key}
         */
        private String prefix = "mango:kv";

        /**
         * Runtime environment segment. Keep dev/test/prod data isolated.
         */
        private String env = "default";

        /**
         * Optional app segment. Disabled by default so infra KV can be shared across applications.
         */
        private boolean appEnabled;

        /**
         * Optional app segment value when appEnabled=true.
         */
        private String app = "app";
    }

    /**
     * Capability bean default assembly switches.
     */
    @Data
    public static class Capability {
        private boolean enabled;
        private boolean cache;
        private boolean locker;
        private boolean counter;
        private boolean rateLimiter;
        private boolean idempotent;
        private boolean tokenStore;
        private boolean idGenerator;
        private boolean serializer;
        private boolean converter;
    }
}
