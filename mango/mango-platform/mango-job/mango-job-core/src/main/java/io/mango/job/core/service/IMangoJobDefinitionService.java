package io.mango.job.core.service;

import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.vo.MangoJobDefinitionVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;

/**
 * Job 任务定义内部服务。
 */
public interface IMangoJobDefinitionService {

    PageResult<MangoJobDefinitionVO> pageDefinitions(MangoJobDefinitionPageQuery query);

    MangoJobDefinitionVO detailDefinition(Long id);

    Long createDefinition(SaveMangoJobDefinitionCommand command);

    Boolean updateDefinition(SaveMangoJobDefinitionCommand command);

    Boolean updateDefinitionStatus(UpdateMangoJobDefinitionStatusCommand command);

    Boolean deleteDefinition(Long id);

    Long triggerDefinition(TriggerMangoJobCommand command);

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
