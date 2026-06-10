package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 事件实体。
 */
@Getter
@Setter
@TableName("mango_job_event")
public class MangoJobEventEntity extends TenantEntity {

    private Long jobId;

    private Long instanceId;

    private Long attemptId;

    private Long workerId;

    private String eventType;

    private LocalDateTime eventTime;

    private String traceId;

    private String payload;
}
