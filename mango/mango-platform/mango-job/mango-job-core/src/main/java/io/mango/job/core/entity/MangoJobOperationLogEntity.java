package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Job 操作日志实体。
 */
@Getter
@Setter
@TableName("mango_job_operation_log")
public class MangoJobOperationLogEntity extends TenantEntity {

    private Long jobId;

    private Long instanceId;

    private String operationType;

    private Long operatorId;

    private String operatorName;

    private String requestSummary;

    private String resultStatus;

    private String errorSummary;

    private String traceId;
}
