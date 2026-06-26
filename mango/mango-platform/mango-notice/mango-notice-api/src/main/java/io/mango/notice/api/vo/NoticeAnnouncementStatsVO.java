package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告统计")
public class NoticeAnnouncementStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "接收人数")
    private Long recipientCount;

    @Schema(description = "已读人数")
    private Long readCount;

    @Schema(description = "待确认人数")
    private Long pendingConfirmCount;

    @Schema(description = "已确认人数")
    private Long confirmedCount;
}
