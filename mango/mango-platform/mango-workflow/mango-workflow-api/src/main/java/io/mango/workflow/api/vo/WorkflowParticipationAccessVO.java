package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowParticipantType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 当前用户对业务坐标的只读参与事实。 */
@Data
@Schema(description = "工作流参与可读性结果")
public class WorkflowParticipationAccessVO {
    @Schema(description = "当前用户是否可读取业务")
    private boolean readable;
    @Schema(description = "当前用户参与类型列表")
    private List<WorkflowParticipantType> participantTypes = List.of();
    @Schema(description = "最近参与的流程实例ID")
    private String latestProcessInstanceId;

    public List<WorkflowParticipantType> getParticipantTypes() {
        return List.copyOf(participantTypes);
    }

    public void setParticipantTypes(List<WorkflowParticipantType> participantTypes) {
        this.participantTypes = participantTypes == null ? List.of() : List.copyOf(participantTypes);
    }
}
