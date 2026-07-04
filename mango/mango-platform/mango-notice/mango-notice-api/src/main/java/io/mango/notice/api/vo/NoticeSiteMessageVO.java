package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Schema(description = "系统消息场景")
    private String messageScene;

    @Schema(description = "系统消息业务对象")
    private NoticeSiteMessageSubjectVO subject;

    @Schema(description = "系统消息跳转目标")
    private NoticeSiteMessageTargetVO target;

    @Schema(description = "系统消息业务数据快照")
    private Map<String, Object> data;

    @Schema(description = "系统消息动作")
    private List<NoticeSiteMessageActionVO> actions;

    @Schema(description = "系统消息过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "通知优先级")
    private NoticePriority priority;

    @Schema(description = "已读状态")
    private NoticeReadStatus readStatus;

    @Schema(description = "已读时间")
    private LocalDateTime readTime;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务域")
    private String bizGroup;

    @Schema(description = "业务消息名称")
    private String bizName;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
