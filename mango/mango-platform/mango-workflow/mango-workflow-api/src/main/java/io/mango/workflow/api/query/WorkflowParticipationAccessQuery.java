package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 单业务坐标的参与可读性查询。 */
@Data
@Schema(description = "工作流参与可读性查询")
public class WorkflowParticipationAccessQuery {
    @Schema(description = "流程定义编码")
    @NotBlank
    @Size(max = 128)
    private String processKey;
    @Schema(description = "业务主键")
    @NotBlank
    @Size(max = 128)
    private String businessKey;
}
