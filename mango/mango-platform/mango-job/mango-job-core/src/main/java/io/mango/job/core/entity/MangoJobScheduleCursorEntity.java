package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 调度游标实体。
 */
@Getter
@Setter
@TableName("mango_job_schedule_cursor")
public class MangoJobScheduleCursorEntity extends TenantEntity {

    private Long jobId;

    private Integer scheduleVersion;

    private LocalDateTime lastFireTime;

    private LocalDateTime nextFireTime;

    private String misfirePolicy;

    private String lockOwner;

    private LocalDateTime lockUntil;

    private LocalDateTime lastScanAt;
}
