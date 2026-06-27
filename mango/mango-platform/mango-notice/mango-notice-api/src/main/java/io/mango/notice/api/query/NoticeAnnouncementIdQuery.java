package io.mango.notice.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告ID查询")
public class NoticeAnnouncementIdQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "公告ID不能为空")
    @Schema(description = "公告ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
