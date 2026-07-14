package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSendMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "发送通知命令")
public class SendNoticeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务对象ID")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizId;

    @Schema(description = "业务参数")
    @Valid
    private NoticeJsonRequest params;

    @Schema(description = "系统消息场景")
    @jakarta.validation.constraints.Size(max = 65535)
    private String messageScene;

    @Valid
    @Schema(description = "系统消息业务对象")
    private NoticeSiteMessageSubjectCommand messageSubject;

    @Valid
    @Schema(description = "系统消息跳转目标")
    private NoticeSiteMessageTargetCommand messageTarget;

    @Schema(description = "系统消息业务数据快照")
    @Valid
    private NoticeJsonRequest messageData;

    @Valid
    @Schema(description = "系统消息交互动作")
    private List<NoticeSiteMessageActionCommand> messageActions;

    @Schema(description = "系统消息过期时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime messageExpireTime;

    @Schema(description = "本次指定发送渠道，空表示按业务类型启用模板发送")
    @jakarta.validation.constraints.Size(max = 1000)
    private List<NoticeChannelType> channelTypes;

    @Schema(description = "接收人列表")
    @Valid
    private List<NoticeRecipientCommand> recipients;

    @Schema(description = "接收目标列表，支持用户、部门、岗位、角色")
    @Valid
    private List<NoticeRecipientTargetCommand> recipientTargets;

    @Schema(description = "接收用户ID，兼容单用户快捷发送")
    @jakarta.validation.constraints.Positive
    private Long userId;

    @Schema(description = "接收用户ID列表，兼容批量用户快捷发送")
    @jakarta.validation.constraints.Size(max = 1000)
    private List<Long> userIds;

    @Schema(description = "接收人规则编码")
    @jakarta.validation.constraints.Size(max = 65535)
    private String recipientRuleCode;

    @Schema(description = "通知标题，未配置业务模板时用于直接发送")
    @jakarta.validation.constraints.Size(max = 65535)
    private String title;

    @Schema(description = "通知内容，未配置业务模板时用于直接发送")
    @jakarta.validation.constraints.Size(max = 65535)
    private String content;

    @Schema(description = "附件文件 ID 列表，仅传文件中心标识")
    @jakarta.validation.constraints.Size(max = 1000)
    private List<Long> attachmentFileIds;

    @Schema(description = "通知优先级")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticePriority priority = NoticePriority.NORMAL;

    @Schema(description = "发送模式")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticeSendMode sendMode = NoticeSendMode.IMMEDIATE;

    @Schema(description = "定时发送时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime scheduledTime;

    @Schema(description = "幂等键")
    @jakarta.validation.constraints.Size(max = 65535)
    private String idempotentKey;
}
