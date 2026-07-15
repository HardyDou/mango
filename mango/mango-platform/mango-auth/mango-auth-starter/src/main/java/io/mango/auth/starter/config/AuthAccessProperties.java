package io.mango.auth.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证过滤链读取的边界白名单兼容配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.access")
public class AuthAccessProperties {

    private IpWhitelist ipWhitelist = new IpWhitelist();

    @Data
    public static class IpWhitelist {
        private boolean enabled;
        private List<Rule> rules = new ArrayList<>();
    }

    @Data
    public static class Rule {
        private String pathPattern;
        private List<String> methods = new ArrayList<>();
        private List<String> cidrs = new ArrayList<>();
    }
}
