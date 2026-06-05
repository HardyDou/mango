package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job Worker 运行态快照实体。
 */
@Getter
@Setter
@TableName("mango_job_worker_snapshot")
public class MangoJobWorkerSnapshotEntity extends TenantEntity {

    private String appCode;

    private String workerAddress;

    private String engineType;

    private String engineWorkerId;

    private LocalDateTime lastHeartbeatAt;

    private String status;
}
