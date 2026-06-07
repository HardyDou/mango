package io.mango.job.api.handler;

/**
 * Mango 原生 Job 处理器契约。
 * <p>
 * 业务模块实现本接口，不直接依赖底层调度引擎处理器类型。
 */
public interface MangoJobHandler {

    /**
     * 所属逻辑应用。默认跟随当前 Mango 上下文；远程 Worker 可显式覆盖。
     *
     * @return 逻辑应用编码；返回 null 表示使用当前上下文
     */
    default String appCode() {
        return null;
    }

    /**
     * 处理器名称，在所属应用内唯一。
     *
     * @return 处理器名称
     */
    String handlerName();

    /**
     * 执行业务任务。
     *
     * @param context Job 执行上下文
     * @return 执行结果
     */
    MangoJobHandleResult handle(MangoJobHandleContext context);
}
