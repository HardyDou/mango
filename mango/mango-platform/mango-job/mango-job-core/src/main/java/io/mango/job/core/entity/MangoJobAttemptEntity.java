package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 执行尝试实体。
 */
@Getter
@Setter
@TableName("mango_job_attempt")
public class MangoJobAttemptEntity extends TenantEntity {

    private Long instanceId;

    private Long jobId;

    private Integer attemptNo;

    private Long workerId;

    private String workerAddressSnapshot;

    private String status;

    private String leaseOwner;

    private LocalDateTime leaseUntil;

    private String fencingToken;

    private LocalDateTime dispatchTime;

    private LocalDateTime startTime;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime endTime;

    private String exitCode;

    private String errorSummary;

    private String resultPayload;
}
