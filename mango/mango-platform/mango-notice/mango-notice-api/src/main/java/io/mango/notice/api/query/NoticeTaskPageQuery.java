package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知任务分页查询")
public class NoticeTaskPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private long pageNum = 1;

    @Schema(description = "每页数量")
    private long pageSize = 10;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务对象ID")
    private String bizId;

    @Schema(description = "任务状态")
    private NoticeTaskStatus status;
}
