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
    @jakarta.validation.constraints.Positive
    private long pageNum = 1;

    @Schema(description = "每页数量")
    @jakarta.validation.constraints.Positive
    private long pageSize = 10;

    @Schema(description = "任务ID")
    @jakarta.validation.constraints.Positive
    private Long taskId;

    @Schema(description = "业务类型")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizType;

    @Schema(description = "业务对象ID")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizId;

    @Schema(description = "业务域")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizGroup;

    @Schema(description = "消息名称")
    @jakarta.validation.constraints.Size(max = 65535)
    private String messageName;

    @Schema(description = "渠道类型")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticeChannelType channelType;

    @Schema(description = "发送状态")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticeSendStatus status;

    @Schema(description = "接收人关键字")
    @jakarta.validation.constraints.Size(max = 65535)
    private String recipientKeyword;

    @Schema(description = "发送开始时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime startTime;

    @Schema(description = "发送结束时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime endTime;
}
