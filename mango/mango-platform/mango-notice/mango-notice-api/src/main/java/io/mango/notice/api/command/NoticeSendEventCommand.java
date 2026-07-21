package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Transactional event contract for requesting asynchronous notice delivery.
 *
 * <p>The event intentionally reuses the canonical send command fields so event
 * and direct API delivery cannot drift into two protocols.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Schema(description = "通知发送事件命令")
public class NoticeSendEventCommand extends SendNoticeCommand {

    private static final long serialVersionUID = 1L;

    @Schema(description = "事件所属租户 ID，仅用于恢复事务提交后的内部调用上下文")
    @NotBlank(message = "事件租户 ID 不能为空")
    private String tenantId;

    @Schema(description = "事件所属应用编码，仅用于恢复事务提交后的内部调用上下文")
    @Size(max = 64, message = "事件应用编码最多64个字符")
    private String appCode;

    @Schema(description = "事件所属登录域，仅用于恢复事务提交后的内部调用上下文")
    @Size(max = 32, message = "事件登录域最多32个字符")
    private String realm;
}
