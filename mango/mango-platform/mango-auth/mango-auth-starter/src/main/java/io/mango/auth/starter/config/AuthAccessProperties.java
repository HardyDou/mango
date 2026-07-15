package io.mango.auth.starter.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证过滤链读取的边界白名单兼容配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.access")
public class AuthAccessProperties {

    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Spring Boot configuration binding requires this mutable getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring Boot configuration binding requires this mutable setter"))
    private IpWhitelist ipWhitelist = new IpWhitelist();

    @Data
    public static class IpWhitelist {
        private boolean enabled;
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring Boot configuration binding requires this mutable getter"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring Boot configuration binding requires this mutable setter"))
        private List<Rule> rules = new ArrayList<>();
    }

    @Data
    public static class Rule {
        private String pathPattern;
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring Boot configuration binding requires this mutable getter"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring Boot configuration binding requires this mutable setter"))
        private List<String> methods = new ArrayList<>();
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring Boot configuration binding requires this mutable getter"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring Boot configuration binding requires this mutable setter"))
        private List<String> cidrs = new ArrayList<>();
    }
}
