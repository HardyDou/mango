package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 逻辑应用集成模块保存命令。
 */
@Data
@Schema(description = "逻辑应用集成模块保存命令")
public class AppModuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "绑定ID，创建时为空")
    private Long bindingId;

    @NotBlank
    @Schema(description = "逻辑应用编码")
    private String appCode;

    @NotBlank
    @Schema(description = "能力模块编码，来自 module.properties 的 module-name")
    private String moduleCode;

    @Schema(description = "能力模块名称")
    private String moduleName;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer sort;
}
