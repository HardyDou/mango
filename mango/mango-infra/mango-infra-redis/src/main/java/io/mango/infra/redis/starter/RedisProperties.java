package io.mango.infra.redis.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Properties
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.redis")
public class RedisProperties {

    /**
     * Enable Redis
     */
    private boolean enabled = true;

    /**
     * Redis host
     */
    private String host = "localhost";

    /**
     * Redis port
     */
    private int port = 6379;

    /**
     * Redis password
     */
    private String password;

    /**
     * Database index
     */
    private int database = 0;

    /**
     * Connection timeout in milliseconds
     */
    private int timeout = 3000;

    /**
     * Pool configuration
     */
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private int maxWait = -1;
    }
}
