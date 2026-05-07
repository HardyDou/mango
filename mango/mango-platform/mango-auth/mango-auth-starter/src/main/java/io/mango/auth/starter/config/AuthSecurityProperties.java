package io.mango.auth.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证安全链配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.auth.security")
public class AuthSecurityProperties {

    /**
     * Spring Security 层直接放行的公共路径。
     */
    private List<String> permitPaths = new ArrayList<>();
}
