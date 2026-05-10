package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程分组视图。
 */
@Data
@Schema(description = "流程分组视图")
public class WorkflowGroupVO {

    @Schema(description = "流程分组ID")
    private Long id;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "分组编码")
    private String groupCode;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
