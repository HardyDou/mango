package io.mango.job.core.service.engine;

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
     * 检查引擎健康状态。
     *
     * @return 健康检查结果
     */
    MangoJobEngineResult health();
}
