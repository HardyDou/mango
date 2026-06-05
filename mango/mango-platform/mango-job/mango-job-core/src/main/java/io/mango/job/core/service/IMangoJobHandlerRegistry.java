package io.mango.job.core.service;

import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.vo.MangoJobHandlerVO;

import java.util.List;

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
}
