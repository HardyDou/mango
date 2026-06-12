package io.mango.infra.event.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重新投递系统事件命令。
 */
@Data
@Schema(description = "重新投递系统事件命令")
public class ReconsumeSystemEventCommand {

    @NotBlank(message = "消息 ID 不能为空")
    @Schema(description = "消息 ID")
    private String messageId;
}
