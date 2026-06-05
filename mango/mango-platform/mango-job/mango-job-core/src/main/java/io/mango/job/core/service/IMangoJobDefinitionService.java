package io.mango.job.core.service;

import io.mango.job.core.entity.MangoJobDefinitionEntity;

/**
 * Job 任务定义内部服务。
 */
public interface IMangoJobDefinitionService {

    /**
     * 保存任务定义实体。
     *
     * @param entity 任务定义实体
     * @return 已保存实体
     */
    MangoJobDefinitionEntity saveDefinition(MangoJobDefinitionEntity entity);

    /**
     * 按 ID 查询任务定义。
     *
     * @param id 任务 ID
     * @return 任务定义实体；不存在时返回 null
     */
    MangoJobDefinitionEntity findById(Long id);
}
