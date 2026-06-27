package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "公告视图")
public class NoticeAnnouncementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "公告状态")
    private NoticeAnnouncementStatus status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "有效开始时间")
    private LocalDateTime validStartTime;

    @Schema(description = "有效结束时间")
    private LocalDateTime validEndTime;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "是否需要确认")
    private Boolean confirmRequired;

    @Schema(description = "是否同步系统消息")
    private Boolean syncMessageEnabled;

    @Schema(description = "发布对象快照")
    private List<NoticeAnnouncementTargetVO> targets;

    @Schema(description = "统计信息")
    private NoticeAnnouncementStatsVO stats;

    @Schema(description = "当前用户已读状态")
    private NoticeReadStatus readStatus;

    @Schema(description = "当前用户阅读时间")
    private LocalDateTime readTime;

    @Schema(description = "当前用户确认状态")
    private NoticeAnnouncementConfirmStatus confirmStatus;

    @Schema(description = "当前用户确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
