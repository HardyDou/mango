package io.mango.job.core.service.engine;

import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;

/**
 * Mango Job 引擎同步服务。
 */
public interface IMangoJobEngineSyncService {

    /**
     * 同步任务定义到调度引擎。
     *
     * @param definition 任务定义实体
     * @param action 同步动作
     */
    void syncDefinition(MangoJobDefinitionEntity definition, String action);

    /**
     * 删除调度引擎侧任务定义。
     *
     * @param definition 任务定义实体
     */
    void deleteDefinition(MangoJobDefinitionEntity definition);

    /**
     * 手动触发任务。
     *
     * @param definition 任务定义实体
     * @param instance 执行实例实体
     * @param batchNo 触发批次号
     */
    void trigger(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance, String batchNo);

    /**
     * 使用指定参数手动触发任务。
     *
     * @param definition 任务定义实体
     * @param instance 执行实例实体
     * @param batchNo 触发批次号
     * @param paramValue 本次执行参数 JSON
     */
    void trigger(MangoJobDefinitionEntity definition,
                 MangoJobInstanceEntity instance,
                 String batchNo,
                 String paramValue);

    /**
     * 刷新执行实例状态和日志。
     *
     * @param definition 任务定义实体
     * @param instance 执行实例实体
     */
    void refreshInstance(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance);

    /**
     * 导入指定时间范围内的调度实例。
     *
     * @param definition 任务定义实体
     * @param triggerTimeStart 触发开始时间
     * @param triggerTimeEnd 触发结束时间
     * @param limit 单次导入上限
     */
    void importScheduledInstances(MangoJobDefinitionEntity definition,
                                  java.time.LocalDateTime triggerTimeStart,
                                  java.time.LocalDateTime triggerTimeEnd,
                                  int limit);
}
