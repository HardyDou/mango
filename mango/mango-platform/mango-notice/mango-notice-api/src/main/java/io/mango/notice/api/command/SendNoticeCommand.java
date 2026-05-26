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
import java.util.Map;

@Data
@Schema(description = "发送通知命令")
public class SendNoticeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "业务参数")
    private Map<String, Object> params;

    @Schema(description = "本次指定发送渠道，空表示按业务类型启用模板发送")
    private List<NoticeChannelType> channelTypes;

    @Schema(description = "接收人列表")
    @Valid
    private List<NoticeRecipientCommand> recipients;

    @Schema(description = "接收用户ID，兼容单用户快捷发送")
    private Long userId;

    @Schema(description = "接收用户ID列表，兼容批量用户快捷发送")
    private List<Long> userIds;

    @Schema(description = "接收人规则编码")
    private String recipientRuleCode;

    @Schema(description = "通知标题，未配置业务模板时用于直接发送")
    private String title;

    @Schema(description = "通知内容，未配置业务模板时用于直接发送")
    private String content;

    @Schema(description = "附件文件 ID 列表，仅传文件中心标识")
    private List<Long> attachmentFileIds;

    @Schema(description = "通知优先级")
    private NoticePriority priority = NoticePriority.NORMAL;

    @Schema(description = "发送模式")
    private NoticeSendMode sendMode = NoticeSendMode.IMMEDIATE;

    @Schema(description = "定时发送时间")
    private LocalDateTime scheduledTime;

    @Schema(description = "幂等键")
    private String idempotentKey;
}
