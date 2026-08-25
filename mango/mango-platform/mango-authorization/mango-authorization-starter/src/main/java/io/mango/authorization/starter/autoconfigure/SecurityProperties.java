package io.mango.authorization.starter.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 授权安全默认链配置。
 */
@Getter
@Setter
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
    private List<String> permitPaths = List.of();

    public List<String> getPermitPaths() {
        return List.copyOf(permitPaths);
    }

    public void setPermitPaths(List<String> permitPaths) {
        this.permitPaths = permitPaths == null ? List.of() : List.copyOf(permitPaths);
    }
}
