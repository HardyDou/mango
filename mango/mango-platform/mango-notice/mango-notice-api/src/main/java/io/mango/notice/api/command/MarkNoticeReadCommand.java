package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量标记站内信已读命令")
public class MarkNoticeReadCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "站内信ID列表")
    @NotEmpty(message = "站内信ID列表不能为空")
    private List<Long> ids;
}
