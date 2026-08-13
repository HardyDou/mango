package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "入站消息管理视图")
public class NoticeInboundMessageVO {

    @Schema(description = "入站消息ID")
    private Long id;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "接收渠道")
    private NoticeChannelType channelType;

    @Schema(description = "来源提供方")
    private String providerCode;

    @Schema(description = "来源消息ID")
    private String messageId;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "发送方")
    private String fromAddress;

    @Schema(description = "接收方JSON")
    private String toAddressesJson;

    @Schema(description = "纯文本正文，仅详情返回")
    private String bodyText;

    @Schema(description = "HTML正文源码，仅详情返回")
    private String bodyHtml;

    @Schema(description = "处理状态")
    private NoticeInboundMessageStatus status;

    @Schema(description = "广播事件ID")
    private String eventId;

    @Schema(description = "失败编码")
    private String failureCode;

    @Schema(description = "失败原因")
    private String failureReason;

    @Schema(description = "处理尝试次数")
    private Integer attemptCount;

    @Schema(description = "接收时间")
    private LocalDateTime receivedAt;

    @Schema(description = "处理时间")
    private LocalDateTime processedAt;

    @Schema(description = "附件，仅详情返回")
    private List<NoticeInboundAttachmentVO> attachments;

    public List<NoticeInboundAttachmentVO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<NoticeInboundAttachmentVO> attachments) {
        this.attachments = attachments == null ? null : List.copyOf(attachments);
    }
}
