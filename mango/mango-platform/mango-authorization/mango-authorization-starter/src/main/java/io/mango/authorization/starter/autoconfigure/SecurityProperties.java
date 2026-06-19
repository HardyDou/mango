package io.mango.authorization.starter.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权安全默认链配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.security")
public class SecurityProperties {

    /**
     * Spring Security 层直接放行的公共路径。
     */
    private List<String> permitPaths = new ArrayList<>();
}
