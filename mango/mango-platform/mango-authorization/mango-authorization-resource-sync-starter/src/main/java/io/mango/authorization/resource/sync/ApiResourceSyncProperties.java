package io.mango.authorization.resource.sync;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * API 资源同步配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.authorization.resource-sync")
public class ApiResourceSyncProperties {

    /**
     * 配置文件声明的 API 资源。用于非 Controller 资源或模块级补充资源。
     */
    private List<Resource> resources = new ArrayList<>();

    /**
     * 配置声明的 API 资源。
     */
    @Data
    public static class Resource {

        /**
         * 稳定 Mango 模块名。
         */
        private String moduleName;

        /**
         * HTTP 方法，如 GET、POST、ALL。
         */
        private String httpMethod = "ALL";

        /**
         * 路径模式，支持 /** 和 Spring 风格 {id}。
         */
        private String pathPattern;

        /**
         * 稳定资源编码。未配置时默认 METHOD:path。
         */
        private String resourceCode;

        /**
         * 权限码，accessMode=PERMISSION 时必填。
         */
        private String permissionCode;

        /**
         * 访问模式。
         */
        private ApiResourceAccessMode accessMode;

        /**
         * 描述。
         */
        private String description;
    }
}
