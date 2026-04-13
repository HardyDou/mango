package io.mango.orm.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ORM configuration properties
 */
@Data
@ConfigurationProperties(prefix = "mango.orm")
public class OrmProperties {

    /**
     * Enable ORM auto-configuration
     */
    private boolean enabled = true;
}
