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
 *       prefix: mango:infra:kv
 *       env: default
 *       appEnabled: false
 *     capability:
 *       enabled: false
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mango.kv")
public class KvStoreProperties {

    /**
     * Legacy store type alias. Prefer store.type.
     */
    private String type = "auto";

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

    /**
     * Return configured store type with legacy mango.kv.type fallback.
     *
     * @return normalized store type.
     */
    public String effectiveStoreType() {
        String configured = store.getType();
        if (configured == null || configured.isBlank()) {
            configured = type;
        }
        if (configured == null || configured.isBlank()) {
            return "auto";
        }
        return configured.trim().toLowerCase();
    }

    @Data
    public static class Store {
        /**
         * KV store type: auto / redis / jdbc / memory.
         * db is accepted as a legacy alias of jdbc.
         */
        private String type;
    }

    @Data
    public static class Provider {

        private Redis redis = new Redis();

        private Jdbc jdbc = new Jdbc();

        /**
         * Legacy provider alias. Prefer jdbc.
         */
        private Db db = new Db();

        private Memory memory = new Memory();
    }

    /**
     * RedisKvStore configuration.
     */
    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private int timeout = 3000;
        private Pool pool = new Pool();

        @Data
        public static class Pool {
            private int maxActive = 8;
            private int maxIdle = 8;
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
            private int initialSize = 5;
            private int maxActive = 20;
            private int minIdle = 5;
            private long maxWait = 60000;
            private long timeBetweenEvictionRunsMillis = 60000;
            private long minEvictableIdleTimeMillis = 300000;
            private String validationQuery = "SELECT 1";
            private boolean testWhileIdle = true;
            private boolean testOnBorrow = false;
            private boolean testOnReturn = false;
        }

        @Data
        public static class Hikari {
            private int maxPoolSize = 10;
            private int minIdle = 5;
            private long connectionTimeout = 30000;
            private long idleTimeout = 600000;
            private long maxLifetime = 1800000;
        }
    }

    /**
     * Legacy alias for provider.jdbc.
     */
    @Deprecated
    public static class Db extends Jdbc {
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
        private String prefix = "mango:infra:kv";

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
