package io.mango.job.api.handler;

import java.util.Set;

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
     * 执行服务编码。用于调度中心选择可执行 Worker；默认跟随 appCode。
     *
     * @return 服务编码；返回 null 表示使用 appCode
     */
    default String serviceCode() {
        return appCode();
    }

    /**
     * Worker 分组。用于同一服务下的执行隔离；默认跟随 serviceCode。
     *
     * @return Worker 分组；返回 null 表示使用 serviceCode
     */
    default String workerGroup() {
        return serviceCode();
    }

    /**
     * 当前处理器显式支持的任务编码。空集合表示按处理器维度匹配，不限制 jobCode。
     *
     * @return 支持的任务编码集合
     */
    default Set<String> supportedJobCodes() {
        return Set.of();
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
