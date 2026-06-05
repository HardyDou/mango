package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 执行实例摘要实体。
 */
@Getter
@Setter
@TableName("mango_job_instance")
public class MangoJobInstanceEntity extends TenantEntity {

    private Long jobId;

    private String triggerType;

    private Long triggerUserId;

    private LocalDateTime triggerTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Long durationMillis;

    private String engineType;

    private String engineInstanceId;

    private String errorSummary;

    private String traceId;

    private String triggerBatchNo;
}
