package io.mango.access.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 边界入口配置属性。
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.access")
public class AccessProperties {

    /**
     * 是否启用边界入口认证
     */
    private boolean authEnabled = true;

    /**
     * 开启后，非 PUBLIC/INTERNAL 接口必须携带 permissionCode 参数。
     */
    private boolean requirePermissionCode = false;

    /**
     * 来源 IP 白名单配置。
     */
    private IpWhitelist ipWhitelist = new IpWhitelist();

    @Data
    public static class IpWhitelist {

        /**
         * 是否启用来源 IP 白名单。
         */
        private boolean enabled = false;

        /**
         * 白名单规则。
         */
        private List<Rule> rules = new ArrayList<>();

    }

    @Data
    public static class Rule {

        /**
         * 请求路径匹配表达式，支持 Ant 风格通配符。
         */
        private String pathPattern;

        /**
         * HTTP 方法。为空或包含 ALL 时匹配全部方法。
         */
        private List<String> methods = new ArrayList<>();

        /**
         * 允许访问的 IP 或 CIDR。
         */
        private List<String> cidrs = new ArrayList<>();

    }

}
