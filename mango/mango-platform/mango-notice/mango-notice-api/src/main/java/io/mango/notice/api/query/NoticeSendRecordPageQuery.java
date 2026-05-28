package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通知发送记录分页查询")
public class NoticeSendRecordPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private long pageNum = 1;

    @Schema(description = "每页数量")
    private long pageSize = 10;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "业务域")
    private String bizGroup;

    @Schema(description = "消息名称")
    private String messageName;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "发送状态")
    private NoticeSendStatus status;

    @Schema(description = "接收人关键字")
    private String recipientKeyword;

    @Schema(description = "发送开始时间")
    private LocalDateTime startTime;

    @Schema(description = "发送结束时间")
    private LocalDateTime endTime;
}
