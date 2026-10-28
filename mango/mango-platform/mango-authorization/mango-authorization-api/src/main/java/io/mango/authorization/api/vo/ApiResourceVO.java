package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.ApiResourceSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 资源登记明细。
 */
@Data
@Schema(description = "API 资源登记明细")
public class ApiResourceVO {

    @Schema(description = "记录主键")
    private Long id;

    @Schema(description = "来源模块名，例如 mango-auth")
    private String moduleName;

    @Schema(description = "接口一级路径段所属领域，例如 auth")
    private String domain;

    @Schema(description = "登记来源")
    private ApiResourceSource source;

    @Schema(description = "是否被人工接管：true 时访问模式来自人工配置")
    private Boolean manualOverride;

    @Schema(description = "接管前的访问模式，未接管时为空")
    private String originAccessMode;

    @Schema(description = "HTTP 方法")
    private String httpMethod;

    @Schema(description = "接口路径模式")
    private String pathPattern;

    @Schema(description = "稳定资源编码")
    private String resourceCode;

    @Schema(description = "权限标识，访问模式为 PERMISSION 时有效")
    private String permissionCode;

    @Schema(description = "访问模式：PUBLIC/LOGIN/PERMISSION/INTERNAL")
    private String accessMode;

    @Schema(description = "处理器类名")
    private String handlerClass;

    @Schema(description = "处理器方法名")
    private String handlerMethod;

    @Schema(description = "资源描述")
    private String description;

    @Schema(description = "资源状态：1-启用，0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
