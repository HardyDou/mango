package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 当前用户参与业务分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前用户工作流参与业务分页查询")
public class WorkflowParticipationPageQuery extends PageQuery {
    @Schema(description = "流程定义编码")
    @Size(max = 128)
    private String processKey;
    @Schema(description = "参与时间起始")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private LocalDateTime startTime;
    @Schema(description = "参与时间结束")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private LocalDateTime endTime;
}
