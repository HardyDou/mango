package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程模板导入结果。
 */
@Data
@Schema(description = "流程模板导入结果")
public class WorkflowTemplateImportVO {

    @Schema(description = "导入成功的流程定义ID列表")
    private List<Long> definitionIds = new ArrayList<>();

    @Schema(description = "导入失败或预检失败的明细")
    private List<WorkflowTemplateImportErrorVO> errors = new ArrayList<>();
}
