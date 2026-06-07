package io.mango.job.support.service;

import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.vo.MangoJobHandlerVO;

import java.util.List;
import java.util.Optional;

/**
 * Mango Job 处理器注册表。
 */
public interface IMangoJobHandlerRegistry {

    /**
     * 注册当前应用内置处理器。
     *
     * @param handler 处理器
     */
    void register(MangoJobHandler handler);

    /**
     * 查询已注册处理器元数据。
     *
     * @return 处理器元数据列表
     */
    List<MangoJobHandlerVO> listHandlers();

    /**
     * 按处理器名称查询处理器实例。
     *
     * @param handlerName 处理器名称
     * @return 处理器实例
     */
    Optional<MangoJobHandler> findHandler(String handlerName);

    /**
     * 按应用和处理器名称查询处理器实例。
     *
     * @param appCode 应用编码
     * @param handlerName 处理器名称
     * @return 处理器实例
     */
    Optional<MangoJobHandler> findHandler(String appCode, String handlerName);
}
