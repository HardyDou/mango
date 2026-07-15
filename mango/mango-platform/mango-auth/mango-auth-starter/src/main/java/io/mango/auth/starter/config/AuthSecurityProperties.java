package io.mango.auth.starter.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring Boot configuration binding requires this mutable getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring Boot configuration binding requires this mutable setter"))
    private List<String> permitPaths = new ArrayList<>();
}
