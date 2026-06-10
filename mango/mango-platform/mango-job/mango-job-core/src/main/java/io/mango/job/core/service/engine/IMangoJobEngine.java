package io.mango.job.core.service.engine;

import java.util.List;

/**
 * Mango Job 底层调度引擎 SPI。
 */
public interface IMangoJobEngine {

    /**
     * 返回引擎类型，必须对应 JobEngineType 枚举名称。
     *
     * @return 引擎类型
     */
    String engineType();

    /**
     * 同步任务定义到引擎。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    MangoJobEngineResult syncDefinition(MangoJobEngineRequest request);

    /**
     * 删除引擎侧任务。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    MangoJobEngineResult deleteDefinition(MangoJobEngineRequest request);

    /**
     * 手动触发引擎侧任务。
     *
     * @param request 触发请求
     * @return 触发结果
     */
    MangoJobEngineResult trigger(MangoJobTriggerRequest request);

    /**
     * 刷新引擎侧实例运行状态。
     *
     * @param request 实例请求
     * @return 实例状态结果
     */
    default MangoJobEngineResult refreshInstance(MangoJobTriggerRequest request) {
        return MangoJobEngineResult.success();
    }

    /**
     * 导入引擎侧已产生的调度实例。
     *
     * @param request 实例导入请求
     * @return 引擎实例快照
     */
    default List<MangoJobEngineInstanceSnapshot> importInstances(MangoJobInstanceImportRequest request) {
        return List.of();
    }

    /**
     * 查询引擎侧执行日志或任务输出。
     *
     * @param request 日志请求
     * @return 日志结果
     */
    default MangoJobLogResult fetchLog(MangoJobLogRequest request) {
        return MangoJobLogResult.failed(engineType(), "当前调度引擎不支持日志详情查询");
    }

    /**
     * 检查引擎健康状态。
     *
     * @return 健康检查结果
     */
    MangoJobEngineResult health();
}
