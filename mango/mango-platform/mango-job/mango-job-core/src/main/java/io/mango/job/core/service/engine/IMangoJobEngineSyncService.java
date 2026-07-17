package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;

/**
 * Mango Job 引擎同步服务。
 */
public interface IMangoJobEngineSyncService {

    /**
     * 同步任务定义到调度引擎。
     *
     * @param context 任务定义同步上下文
     */
    void syncDefinition(MangoJobEngineDefinitionContext context);

    /**
     * 删除调度引擎侧任务定义。
     *
     * @param definition 任务定义实体
     */
    void deleteDefinition(MangoJobDefinitionEntity definition);

    /**
     * 手动触发任务。
     *
     * @param context 任务触发上下文
     */
    void trigger(MangoJobEngineTriggerContext context);

    /**
     * 刷新执行实例状态和日志。
     *
     * @param context 实例刷新上下文
     */
    void refreshInstance(MangoJobEngineTriggerContext context);

    /**
     * 导入指定时间范围内的调度实例。
     *
     * @param criteria 实例导入条件
     */
    void importScheduledInstances(MangoJobInstanceImportCriteria criteria);
}
