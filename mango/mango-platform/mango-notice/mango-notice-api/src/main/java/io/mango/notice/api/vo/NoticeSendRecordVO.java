package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通知发送记录")
public class NoticeSendRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "接收人ID")
    private Long recipientId;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "接收人名称")
    private String recipientName;

    @Schema(description = "接收人账号")
    private String recipientAccount;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务域")
    private String bizGroup;

    @Schema(description = "消息名称")
    private String messageName;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "业务渠道模板ID")
    private Long businessChannelTemplateId;

    @Schema(description = "业务渠道模板名称")
    private String businessChannelTemplateName;

    @Schema(description = "模板版本")
    private Integer templateVersion;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "通道配置ID")
    private Long channelConfigId;

    @Schema(description = "通道名称")
    private String channelConfigName;

    @Schema(description = "请求流水号")
    private String requestId;

    @Schema(description = "发送状态")
    private NoticeSendStatus status;

    @Schema(description = "渲染后标题")
    private String renderedTitle;

    @Schema(description = "渲染后内容")
    private String renderedContent;

    @Schema(description = "请求摘要 JSON")
    private String requestSnapshot;

    @Schema(description = "响应摘要 JSON")
    private String responseSnapshot;

    @Schema(description = "供应商消息 ID")
    private String providerMessageId;

    @Schema(description = "失败码")
    private String failCode;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt;
}
