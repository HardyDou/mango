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
     * 是否装配 Authorization starter 的兜底安全链。
     * 完整管理端由 Auth starter 提供主安全链时必须关闭。
     */
    private boolean defaultChainEnabled = true;

    /**
     * Spring Security 层直接放行的公共路径。
     */
    private List<String> permitPaths = new ArrayList<>();
}
