package io.mango.job.core.service.engine;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 引擎侧实例快照。
 */
@Getter
@Setter
public class MangoJobEngineInstanceSnapshot {

    private String engineInstanceId;

    private LocalDateTime triggerTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Long durationMillis;

    private String errorSummary;

    private String workerAddress;

    private String triggerBatchNo;

    public boolean hasEngineInstanceId() {
        return engineInstanceId != null && !engineInstanceId.isBlank();
    }
}
