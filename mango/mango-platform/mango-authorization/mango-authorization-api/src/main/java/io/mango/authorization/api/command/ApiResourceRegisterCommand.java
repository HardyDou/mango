package io.mango.authorization.api.command;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 资源注册命令。
 */
@Data
@Schema(description = "API 资源注册命令")
public class ApiResourceRegisterCommand {

    /**
     * 稳定 Mango 模块名，不随部署服务变化。
     */
    @Schema(description = "稳定 Mango 模块名，不随部署服务变化")
    private String moduleName;

    /**
     * HTTP 方法，如 GET、POST。
     */
    @Schema(description = "HTTP 方法，如 GET、POST")
    private String httpMethod;

    /**
     * Spring MVC 路径模式。
     */
    @Schema(description = "Spring MVC 路径模式")
    private String pathPattern;

    /**
     * 稳定资源编码，未声明权限码时默认使用 METHOD:path。
     */
    @Schema(description = "稳定资源编码，未声明权限码时默认使用 METHOD:path")
    private String resourceCode;

    /**
     * 权限码，通常来自 @ApiAccess(PERMISSION)。
     */
    @Schema(description = "权限码，访问模式为 PERMISSION 时必填")
    private String permissionCode;

    /**
     * 访问模式，由授权资源配置控制。
     */
    @Schema(description = "访问模式")
    private ApiResourceAccessMode accessMode;

    /**
     * 处理器类名。
     */
    @Schema(description = "处理器类名")
    private String handlerClass;

    /**
     * 处理器方法名。
     */
    @Schema(description = "处理器方法名")
    private String handlerMethod;

    /**
     * 资源中文描述。
     */
    @Schema(description = "资源描述")
    private String description;
}
