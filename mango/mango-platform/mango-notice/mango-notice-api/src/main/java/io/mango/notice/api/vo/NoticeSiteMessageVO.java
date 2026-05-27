package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统消息视图")
public class NoticeSiteMessageVO {

    @Schema(description = "系统消息ID")
    private Long id;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "通知优先级")
    private NoticePriority priority;

    @Schema(description = "已读状态")
    private NoticeReadStatus readStatus;

    @Schema(description = "已读时间")
    private LocalDateTime readTime;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
