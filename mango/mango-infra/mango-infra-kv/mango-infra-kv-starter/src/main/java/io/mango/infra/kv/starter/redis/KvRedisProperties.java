package io.mango.infra.kv.starter.redis;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Properties
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.redis")
public class KvRedisProperties {

    static final int DEFAULT_PORT = 6379;
    static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    static final int DEFAULT_MAX_ACTIVE = 8;
    static final int DEFAULT_MAX_IDLE = 8;

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
    private int port = DEFAULT_PORT;

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
    private int timeout = DEFAULT_TIMEOUT_MILLIS;

    /**
     * Pool configuration
     */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally stores this nested property bean"))
    private Pool pool = new Pool();

    @Data
    public static class Pool {
        private int maxActive = DEFAULT_MAX_ACTIVE;
        private int maxIdle = DEFAULT_MAX_IDLE;
        private int minIdle = 0;
        private int maxWait = -1;
    }
}
