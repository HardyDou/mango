package io.mango.kv.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KV Store configuration properties.
 */
@Data
@ConfigurationProperties(prefix = "mango.kv")
public class KvStoreProperties {

    /**
     * KV store type: auto / redis / db / memory
     */
    private String storeType = "auto";
}
