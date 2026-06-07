package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Job Worker 能力实体。
 */
@Getter
@Setter
@TableName("mango_job_worker_capability")
public class MangoJobWorkerCapabilityEntity extends TenantEntity {

    private Long workerId;

    private String appCode;

    private String serviceCode;

    private String workerGroup;

    private String jobCode;

    private String handlerName;

    private String handlerVersion;

    private String paramSchemaHash;

    private Integer enabled;
}
