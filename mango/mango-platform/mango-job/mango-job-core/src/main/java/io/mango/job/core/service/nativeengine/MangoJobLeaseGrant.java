package io.mango.job.core.service.nativeengine;

import io.mango.job.core.entity.MangoJobAttemptEntity;

/**
 * Job 执行租约授予参数。
 */
public record MangoJobLeaseGrant(MangoJobAttemptEntity attempt,
                                 String leaseOwner,
                                 long leaseSeconds) {
}
