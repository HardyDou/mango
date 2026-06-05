package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job Worker 快照返回对象。
 */
@Data
@Schema(description = "Job Worker 快照返回对象")
public class MangoJobWorkerSnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Worker 快照 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "Worker 地址")
    private String workerAddress;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "引擎 Worker 标识")
    private String engineWorkerId;

    @Schema(description = "最近心跳时间")
    private LocalDateTime lastHeartbeatAt;

    @Schema(description = "Worker 状态")
    private String status;
}
