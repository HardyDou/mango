package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量人工处理通知发送记录命令")
public class HandleNoticeSendRecordsCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "发送记录 ID 不能为空")
    @Schema(description = "发送记录 ID 列表")
    private List<Long> ids;

    @NotBlank(message = "处理原因不能为空")
    @Schema(description = "处理原因")
    private String reason;
}
