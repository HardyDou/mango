package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逻辑应用集成模块 VO。
 */
@Data
@Schema(description = "逻辑应用集成模块")
public class AppModuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用模块绑定ID")
    private Long bindingId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "能力模块编码")
    private String moduleCode;
    @Schema(description = "能力模块名称")
    private String moduleName;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
