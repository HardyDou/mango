package io.mango.gateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway配置属性
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.gateway")
public class GatewayProperties {

    /**
     * 是否启用网关认证
     */
    private boolean authEnabled = true;

    /**
     * JWT密钥
     */
    private String jwtSecret = "mango-secret-key-change-in-production";

    /**
     * Token过期时间(秒)
     */
    private long tokenExpireSeconds = 7200;

    /**
     * 路由配置
     */
    private Routes routes = new Routes();

    @Data
    public static class Routes {
        /**
         * 认证服务地址 (单体模式直接URL，微服务模式用 lb://service-name)
         */
        private String authUrl = "http://localhost:8081";

        /**
         * BFF Admin服务地址
         */
        private String bffAdminUrl = "http://localhost:8082";

        /**
         * AI服务地址
         */
        private String aiUrl = "http://localhost:8083";

        /**
         * 用户服务地址
         */
        private String userUrl = "http://localhost:8084";

        /**
         * I18n服务地址
         */
        private String i18nUrl = "http://localhost:8085";

        /**
         * 行政区划服务地址
         */
        private String areaUrl = "http://localhost:8086";

        /**
         * 是否使用服务发现 (true=lb://, false=直接URL)
         */
        private boolean useDiscovery = false;

        /**
         * 认证服务 (微服务模式)
         */
        private String authService = "mango-auth-starter";

        /**
         * BFF Admin服务 (微服务模式)
         */
        private String bffAdminService = "mango-admin-app";

        /**
         * AI服务 (微服务模式)
         */
        private String aiService = "mango-ai-starter";

        /**
         * 用户服务 (微服务模式)
         */
        private String userService = "mango-user-starter";

        /**
         * I18n服务 (微服务模式)
         */
        private String i18nService = "mango-i18n-starter";

        /**
         * 行政区划服务 (微服务模式)
         */
        private String areaService = "mango-area-starter";
    }
}
