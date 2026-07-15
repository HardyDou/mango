package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端模块运行策略 VO。
 */
@Data
@Schema(description = "前端模块运行策略")
public class FrontendModuleRuntimeStrategyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "运行策略ID")
    private Long strategyId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "能力模块编码")
    private String moduleCode;
    @Schema(description = "部署配置档")
    private String deployProfile;
    @Schema(description = "页面运行类型")
    private String pageType;
    @Schema(description = "前端运行单元编码")
    private String runtimeCode;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
