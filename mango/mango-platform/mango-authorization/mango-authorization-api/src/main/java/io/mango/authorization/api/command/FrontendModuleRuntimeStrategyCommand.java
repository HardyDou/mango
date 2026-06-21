package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 前端模块运行策略保存命令。
 */
@Data
@Schema(description = "前端模块运行策略保存命令")
public class FrontendModuleRuntimeStrategyCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "策略ID，创建时为空")
    private Long strategyId;

    @NotBlank
    @Schema(description = "逻辑应用编码")
    private String appCode;

    @NotBlank
    @Schema(description = "能力模块编码")
    private String moduleCode;

    @NotBlank
    @Schema(description = "部署配置档：monolith/hybrid/micro")
    private String deployProfile;

    @NotBlank
    @Schema(description = "页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK")
    private String pageType;

    @NotBlank
    @Schema(description = "前端运行单元编码，关联 authorization_frontend_app_registry.app_code")
    private String runtimeCode;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer sort;
}
