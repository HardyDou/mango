package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "发布公告命令")
public class PublishNoticeAnnouncementCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "公告ID不能为空")
    @Schema(description = "公告ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "有效开始时间。不传则使用公告草稿值")
    private LocalDateTime validStartTime;

    @Schema(description = "有效结束时间。不传则使用公告草稿值")
    private LocalDateTime validEndTime;

    @Schema(description = "是否置顶。不传则使用公告草稿值")
    private Boolean pinned;

    @Schema(description = "是否需要用户确认。不传则使用公告草稿值")
    private Boolean confirmRequired;

    @Schema(description = "是否同步生成系统消息提醒。不传则使用公告草稿值")
    private Boolean syncMessageEnabled;

    @Valid
    @Schema(description = "发布对象。不传则使用公告草稿对象")
    private List<NoticeAnnouncementTargetCommand> targets;
}
