package io.mango.job.core.service.nativeengine;

import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.service.engine.MangoJobEngineTriggerContext;
import io.mango.job.core.service.engine.MangoJobInstanceImportCriteria;

/**
 * Mango 原生 Job 运行时。
 */
public interface IMangoNativeJobRuntime {

    /**
     * 同步任务定义到原生运行时。
     *
     * @param definition 任务定义实体
     */
    void syncDefinition(MangoJobDefinitionEntity definition);

    /**
     * 删除原生运行时任务定义。
     *
     * @param definition 任务定义实体
     */
    void deleteDefinition(MangoJobDefinitionEntity definition);

    /**
     * 触发原生任务执行。
     *
     * @param context 任务触发上下文
     */
    void trigger(MangoJobEngineTriggerContext context);

    /**
     * 扫描并派发到期调度任务。
     */
    void tick();

    /**
     * 注册当前进程内嵌 Worker。
     *
     * @param tenantId 调度租户 ID
     */
    void registerEmbeddedWorkers(String tenantId);

    /**
     * 导入指定时间范围内的调度实例。
     *
     * @param criteria 实例导入条件
     */
    void importScheduledInstances(MangoJobInstanceImportCriteria criteria);

    /**
     * 查询执行实例日志详情。
     *
     * @param instanceId 执行实例 ID
     * @return 日志详情
     */
    MangoJobLogDetailVO detailInstanceLog(Long instanceId);
}
