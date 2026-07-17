package io.mango.job.core.service.nativeengine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;

/**
 * Job 失败告警上下文。
 */
public record MangoJobAlarmContext(MangoJobDefinitionEntity definition,
                                   MangoJobInstanceEntity instance,
                                   String errorSummary) {
}
