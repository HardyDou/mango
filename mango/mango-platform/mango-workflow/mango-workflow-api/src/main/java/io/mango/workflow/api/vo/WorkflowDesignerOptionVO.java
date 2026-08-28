package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程设计器候选项。
 */
@Getter
@Setter
@Schema(description = "流程设计器候选项")
public class WorkflowDesignerOptionVO {

    @Schema(description = "候选项稳定值")
    private String value;

    @Schema(description = "候选项显示名称")
    private String label;

    @Schema(description = "子候选项；仅组织树使用")
    private List<WorkflowDesignerOptionVO> children = new ArrayList<>();
}
