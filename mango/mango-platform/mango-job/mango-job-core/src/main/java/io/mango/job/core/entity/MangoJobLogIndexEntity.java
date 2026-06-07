package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 执行日志索引实体。
 */
@Getter
@Setter
@TableName("mango_job_log_index")
public class MangoJobLogIndexEntity extends TenantEntity {

    private Long jobId;

    private Long instanceId;

    private String engineType;

    private String engineInstanceId;

    private String logLocation;

    private Long readOffset;

    private String errorSummary;

    private LocalDateTime lastFetchedAt;
}
