package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 完整替换业务声明的工作流参与人。 */
@Data
@Schema(description = "替换业务工作流参与人命令")
public class ReplaceWorkflowBusinessParticipantsCommand {
    @Schema(description = "流程定义编码")
    @NotBlank
    @Size(max = 128)
    private String processKey;
    @Schema(description = "业务主键")
    @NotBlank
    @Size(max = 128)
    private String businessKey;
    @Schema(description = "流程实例ID")
    @NotBlank
    @Size(max = 128)
    private String processInstanceId;
    @Schema(description = "参与人用户ID列表")
    @NotNull
    @Size(max = 200)
    @Valid
    private List<@NotNull Long> participantUserIds;

    public List<Long> getParticipantUserIds() {
        return participantUserIds == null ? null : List.copyOf(participantUserIds);
    }

    public void setParticipantUserIds(List<Long> participantUserIds) {
        this.participantUserIds = participantUserIds == null ? null : List.copyOf(participantUserIds);
    }
}
