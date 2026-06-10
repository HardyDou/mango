package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job 日志分片实体。
 */
@Getter
@Setter
@TableName("mango_job_log_chunk")
public class MangoJobLogChunkEntity extends TenantEntity {

    private Long instanceId;

    private Long attemptId;

    private Long sequenceNo;

    private LocalDateTime logTime;

    private String level;

    private String loggerName;

    private String threadName;

    private String content;

    private String contentHash;

    private Integer redacted;
}
