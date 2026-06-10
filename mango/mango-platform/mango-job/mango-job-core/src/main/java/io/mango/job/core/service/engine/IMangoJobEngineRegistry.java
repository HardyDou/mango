package io.mango.job.core.service.engine;

import java.util.Optional;

/**
 * Mango Job 引擎注册表。
 */
public interface IMangoJobEngineRegistry {

    /**
     * 按引擎类型查找引擎。
     *
     * @param engineType 引擎类型
     * @return 引擎实例
     */
    Optional<IMangoJobEngine> findEngine(String engineType);
}
