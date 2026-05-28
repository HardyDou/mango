package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知发送记录人工处理命令")
public class HandleNoticeSendRecordCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "处理原因不能为空")
    @Schema(description = "处理原因")
    private String reason;
}
