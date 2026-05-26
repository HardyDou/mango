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

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

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
