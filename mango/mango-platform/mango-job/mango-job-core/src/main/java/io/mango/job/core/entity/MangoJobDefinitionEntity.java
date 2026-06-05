package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Job 任务定义实体。
 */
@Getter
@Setter
@TableName("mango_job_definition")
public class MangoJobDefinitionEntity extends TenantEntity {

    private String appCode;

    private String jobCode;

    private String jobName;

    private String jobType;

    private String scheduleType;

    private String scheduleExpression;

    private String handlerName;

    private String paramSchema;

    private String paramValue;

    private String misfireStrategy;

    private String concurrencyPolicy;

    private Integer timeoutSeconds;

    private String retryPolicy;

    private String status;

    private String engineType;

    private String engineAppId;

    private String engineJobId;

    private String syncStatus;

    private String syncError;
}
