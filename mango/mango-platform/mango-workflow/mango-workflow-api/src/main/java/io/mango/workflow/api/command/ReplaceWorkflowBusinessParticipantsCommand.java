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
    @NotBlank
    @Size(max = 128)
    private String processKey;
    @NotBlank
    @Size(max = 128)
    private String businessKey;
    @NotBlank
    @Size(max = 128)
    private String processInstanceId;
    @NotNull
    @Size(max = 200)
    @Valid
    private List<@NotNull Long> participantUserIds;
}
