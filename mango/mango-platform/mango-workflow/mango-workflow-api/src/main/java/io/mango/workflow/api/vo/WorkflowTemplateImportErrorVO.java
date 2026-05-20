package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程模板导入错误明细。
 */
@Data
@Schema(description = "流程模板导入错误明细")
public class WorkflowTemplateImportErrorVO {

    @Schema(description = "流程模板ID")
    private Long templateId;

    @Schema(description = "流程模板名称")
    private String templateName;

    @Schema(description = "流程模板编码")
    private String templateCode;

    @Schema(description = "错误原因")
    private String reason;
}
