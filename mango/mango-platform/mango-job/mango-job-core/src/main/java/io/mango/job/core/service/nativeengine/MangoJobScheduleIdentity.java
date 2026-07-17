package io.mango.job.core.service.nativeengine;

import java.time.LocalDateTime;

/**
 * 计划任务幂等身份。
 */
public record MangoJobScheduleIdentity(Long jobId,
                                       int scheduleVersion,
                                       LocalDateTime scheduledFireTime) {
}
