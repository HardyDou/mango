package io.mango.authorization.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 前端运行时配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.frontend")
public class FrontendRuntimeProperties {

    /**
     * 当前生效部署配置档：monolith/hybrid/micro。
     */
    private String deployProfile = "monolith";
}
