package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 业务声明参与人结果。 */
@Data
@Schema(description = "工作流业务参与人结果")
public class WorkflowBusinessParticipantsVO {
    @Schema(description = "流程定义编码")
    private String processKey;
    @Schema(description = "业务主键")
    private String businessKey;
    @Schema(description = "流程实例ID")
    private String processInstanceId;
    @Schema(description = "参与人用户ID列表")
    private List<Long> participantUserIds = List.of();

    public List<Long> getParticipantUserIds() {
        return List.copyOf(participantUserIds);
    }

    public void setParticipantUserIds(List<Long> participantUserIds) {
        this.participantUserIds = participantUserIds == null ? List.of() : List.copyOf(participantUserIds);
    }
}
