package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 与底层引擎对象映射实体。
 */
@Getter
@Setter
@TableName("mango_job_engine_mapping")
public class MangoJobEngineMappingEntity extends TenantEntity {

    private Long jobId;

    private Long instanceId;

    private String appCode;

    private String engineType;

    private String engineAppId;

    private String engineJobId;

    private String engineInstanceId;

    private String syncStatus;

    private String syncError;
}
