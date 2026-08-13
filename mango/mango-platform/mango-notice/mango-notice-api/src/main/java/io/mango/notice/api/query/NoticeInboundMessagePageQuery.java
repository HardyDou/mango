package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import io.mango.notice.api.validation.NoticeOptionalValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "入站消息分页查询")
public class NoticeInboundMessagePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive
    @Schema(description = "页码")
    private long pageNum = 1;

    @Positive
    @Schema(description = "每页数量")
    private long pageSize = 10;

    @NotNull(groups = NoticeOptionalValidation.class)
    @Schema(description = "接收渠道")
    private NoticeChannelType channelType;

    @NotNull(groups = NoticeOptionalValidation.class)
    @Schema(description = "处理状态")
    private NoticeInboundMessageStatus status;

    @Size(max = 200)
    @Schema(description = "主题、发送方或来源消息ID关键字")
    private String keyword;

    @NotNull(groups = NoticeOptionalValidation.class)
    @Schema(description = "接收开始时间")
    private LocalDateTime startTime;

    @NotNull(groups = NoticeOptionalValidation.class)
    @Schema(description = "接收结束时间")
    private LocalDateTime endTime;
}
