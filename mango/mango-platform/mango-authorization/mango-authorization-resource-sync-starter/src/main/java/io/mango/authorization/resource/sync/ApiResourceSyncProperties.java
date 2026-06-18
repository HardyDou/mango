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
     * 扫描所属模块名，未配置时使用 unknown-module。
     */
    private String moduleName;

    /**
     * 同步模式。write 注册资源，read 只扫描日志。
     */
    private String mode = "write";

    /**
     * 只扫描这些 Java 包下的 Controller。
     */
    private String includePackages = "io.mango";

    /**
     * 排除路径，支持精确路径和 /** 后缀。
     */
    private String excludePaths = "/error,/actuator/**";

    /**
     * 未声明 @ApiAccess 时的默认访问模式。
     */
    private ApiResourceAccessMode defaultAccessMode = ApiResourceAccessMode.LOGIN;

    /**
     * 生成 mango-resource 声明时使用的来源模块编码。
     */
    private String providerModuleCode = "authorization";

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
