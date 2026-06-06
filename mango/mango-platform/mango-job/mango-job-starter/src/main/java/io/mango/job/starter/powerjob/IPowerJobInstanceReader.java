package io.mango.job.starter.powerjob;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PowerJob 实例只读查询。
 */
public interface IPowerJobInstanceReader {

    List<PowerJobInstanceInfoEntity> readRecentInstances(Long jobId,
                                                         LocalDateTime triggerTimeStart,
                                                         LocalDateTime triggerTimeEnd,
                                                         int limit);
}
