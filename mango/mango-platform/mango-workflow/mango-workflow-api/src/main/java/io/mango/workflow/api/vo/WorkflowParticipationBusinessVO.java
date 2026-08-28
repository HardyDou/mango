package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowParticipantType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 当前用户参与过的业务坐标。 */
@Data
@Schema(description = "工作流参与业务")
public class WorkflowParticipationBusinessVO {
    @Schema(description = "流程定义编码")
    private String processKey;
    @Schema(description = "业务主键")
    private String businessKey;
    @Schema(description = "流程实例ID")
    private String processInstanceId;
    @Schema(description = "当前用户参与类型列表")
    private List<WorkflowParticipantType> participantTypes = List.of();
    @Schema(description = "最近参与时间")
    private LocalDateTime lastParticipatedAt;

    public List<WorkflowParticipantType> getParticipantTypes() {
        return List.copyOf(participantTypes);
    }

    public void setParticipantTypes(List<WorkflowParticipantType> participantTypes) {
        this.participantTypes = participantTypes == null ? List.of() : List.copyOf(participantTypes);
    }
}
