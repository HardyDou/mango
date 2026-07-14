package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "保存公告命令")
public class SaveNoticeAnnouncementCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID。新增时为空，修改时必填")
    @jakarta.validation.constraints.Positive
    private Long id;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200个字符")
    @Schema(description = "公告标题")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "有效开始时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime validStartTime;

    @Schema(description = "有效结束时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime validEndTime;

    @Schema(description = "是否置顶")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean pinned;

    @Schema(description = "是否需要用户确认")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean confirmRequired;

    @Schema(description = "发布时是否同步生成系统消息提醒")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean syncMessageEnabled;

    @Valid
    @Schema(description = "发布对象。草稿可为空，发布时必填")
    private List<NoticeAnnouncementTargetCommand> targets;
}
