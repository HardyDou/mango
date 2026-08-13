package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeInboundAttachmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "入站消息附件视图")
public class NoticeInboundAttachmentVO {

    @Schema(description = "附件记录ID")
    private Long id;

    @Schema(description = "文件中心文件ID")
    private Long fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "附件处理状态")
    private NoticeInboundAttachmentStatus status;

    @Schema(description = "失败原因")
    private String failureReason;
}
