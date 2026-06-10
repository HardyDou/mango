package io.mango.job.core.service.nativeengine;

import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;

import java.time.LocalDateTime;

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
     * @param definition 任务定义实体
     * @param instance 执行实例实体
     * @param batchNo 触发批次号
     * @param paramValue 本次执行参数 JSON
     */
    void trigger(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance, String batchNo, String paramValue);

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
     * @param definition 任务定义实体
     * @param triggerTimeStart 触发开始时间
     * @param triggerTimeEnd 触发结束时间
     * @param limit 单次导入上限
     */
    void importScheduledInstances(MangoJobDefinitionEntity definition,
                                  LocalDateTime triggerTimeStart,
                                  LocalDateTime triggerTimeEnd,
                                  int limit);

    /**
     * 查询执行实例日志详情。
     *
     * @param instanceId 执行实例 ID
     * @return 日志详情
     */
    MangoJobLogDetailVO detailInstanceLog(Long instanceId);
}
