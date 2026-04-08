package io.mango.dal.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DAL store configuration properties.
 */
@Data
@ConfigurationProperties(prefix = "mango.dal.kvstore")
public class DalStoreProperties {

    /**
     * DAL store type: auto / redis / db / memory
     * - auto: auto-detect (RedissonClient → DataSource → Memory)
     * - redis: force RedisXivStore (requires RedissonClient)
     * - db: force DbXivStore (requires DataSource)
     * - memory: force MemoryXivStore (no dependencies)
     */
    private String type = "auto";
}
