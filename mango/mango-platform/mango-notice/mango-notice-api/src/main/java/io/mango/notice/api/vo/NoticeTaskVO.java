package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通知任务")
public class NoticeTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "任务编码")
    private String taskCode;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "渠道集合")
    private String channelTypes;

    @Schema(description = "任务状态")
    private NoticeTaskStatus status;

    @Schema(description = "总数")
    private Integer totalCount;

    @Schema(description = "成功数")
    private Integer successCount;

    @Schema(description = "失败数")
    private Integer failCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
