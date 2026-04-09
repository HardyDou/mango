package io.mango.dal.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DAL store configuration properties.
 *
 * 配置结构：
 * <pre>
 * mango:
 *   dal:
 *     type: auto/redis/db/memory
 *     provider:
 *       redis:
 *         host: localhost
 *         port: 6379
 *         password:
 *         database: 0
 *         timeout: 3000
 *         pool:
 *           maxActive: 8
 *           maxIdle: 8
 *           minIdle: 0
 *           maxWait: -1
 *       db:
 *         tableName: sys_kv_record
 *       memory:
 *         cleanupIntervalMinutes: 1
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mango.dal")
public class DalStoreProperties {

    /**
     * DAL store type: auto / redis / db / memory
     * - auto: auto-detect (RedissonClient → MemoryKvStore)
     * - redis: force RedisKvStore (requires RedissonClient)
     * - db: force DbKvStore (requires DataSource)
     * - memory: force MemoryKvStore (no dependencies)
     */
    private String type = "auto";

    /**
     * Provider-specific configurations
     */
    private Provider provider = new Provider();

    @Data
    public static class Provider {

        private Redis redis = new Redis();

        private Db db = new Db();

        private Memory memory = new Memory();
    }

    /**
     * RedisKvStore configuration
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
     * DbKvStore configuration
     *
     * 数据库连接配置优先级：
     * - mango.dal.provider.db.url        > spring.datasource.url
     * - mango.dal.provider.db.username     > spring.datasource.username
     * - mango.dal.provider.db.password     > spring.datasource.password
     * - mango.dal.provider.db.driver       > spring.datasource.driver-class-name
     * - Druid: mango.dal.provider.db.druid.* > spring.datasource.druid.*
     * - Hikari: mango.dal.provider.db.hikari.* > spring.datasource.hikari.*
     */
    @Data
    public static class Db {
        /** KV 存储表名，默认 sys_kv_record */
        private String tableName = "sys_kv_record";

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
     * MemoryKvStore configuration
     */
    @Data
    public static class Memory {
        /**
         * 过期 key 清理任务间隔（分钟），默认 1
         */
        private int cleanupIntervalMinutes = 1;
    }
}
