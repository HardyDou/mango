package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记抄送已阅命令。
 */
@Data
@Schema(description = "标记抄送已阅命令")
public class ReadWorkflowCopiedTaskCommand {

    @Schema(description = "抄送记录ID")
    @NotNull(message = "抄送记录ID不能为空")
    private Long copiedTaskId;
}
