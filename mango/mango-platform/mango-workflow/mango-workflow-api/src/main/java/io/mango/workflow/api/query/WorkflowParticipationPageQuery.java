package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 当前用户参与业务分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前用户工作流参与业务分页查询")
public class WorkflowParticipationPageQuery extends PageQuery {
    @Size(max = 128)
    private String processKey;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
