package io.mango.biz.notification.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量标记消息已读命令")
public class MarkNotificationReadBatchCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID列表")
    @NotEmpty(message = "消息ID列表不能为空")
    private List<Long> ids;
}
