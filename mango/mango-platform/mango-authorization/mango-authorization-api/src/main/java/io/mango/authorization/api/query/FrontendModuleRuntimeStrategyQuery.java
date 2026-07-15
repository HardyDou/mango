package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 前端模块运行策略查询与定位条件。 */
@Data
@Schema(description = "前端模块运行策略查询与定位条件")
public class FrontendModuleRuntimeStrategyQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 64)
    @Schema(description = "应用编码")
    private String appCode;

    @Size(max = 100)
    @Schema(description = "能力模块编码")
    private String moduleCode;

    @Size(max = 32)
    @Schema(description = "部署配置档")
    private String deployProfile;

    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
