package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告操作命令")
public class NoticeAnnouncementIdCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "公告ID不能为空")
    @Schema(description = "公告ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
