package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 引擎状态返回对象。
 */
@Data
@Schema(description = "Job 引擎状态返回对象")
public class MangoJobEngineStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "同步中任务数量")
    private Long pendingCount;

    @Schema(description = "同步失败任务数量")
    private Long failedCount;

    @Schema(description = "同步成功任务数量")
    private Long syncedCount;

    @Schema(description = "最近更新时间")
    private LocalDateTime lastUpdatedAt;
}
