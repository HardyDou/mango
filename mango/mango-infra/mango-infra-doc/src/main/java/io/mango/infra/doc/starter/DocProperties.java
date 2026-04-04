package io.mango.infra.doc.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API Documentation Properties
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.doc")
public class DocProperties {

    /**
     * Enable API documentation
     */
    private boolean enabled = true;

    /**
     * API title
     */
    private String title = "Mango API";

    /**
     * API description
     */
    private String description = "Mango Scaffold API Documentation";

    /**
     * API version
     */
    private String version = "1.0.0";

    /**
     * OpenAPI group
     */
    private String group = "public-api";

    /**
     * Paths to include in documentation
     */
    private String[] pathsToMatch = {"/api/**"};

    /**
     * Contact information
     */
    private Contact contact = new Contact();

    /**
     * License name
     */
    private String license = "Apache 2.0";

    @Data
    public static class Contact {
        private String name = "Mango Team";
        private String email = "mango@example.com";
    }
}
