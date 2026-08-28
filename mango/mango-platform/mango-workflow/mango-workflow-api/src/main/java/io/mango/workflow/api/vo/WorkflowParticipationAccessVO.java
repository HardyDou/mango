package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowParticipantType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 当前用户对业务坐标的只读参与事实。 */
@Data
@Schema(description = "工作流参与可读性结果")
public class WorkflowParticipationAccessVO {
    private boolean readable;
    private List<WorkflowParticipantType> participantTypes = List.of();
    private String latestProcessInstanceId;
}
