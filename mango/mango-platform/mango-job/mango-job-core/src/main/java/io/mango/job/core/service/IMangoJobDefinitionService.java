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

    /**
     * 分页查询任务定义。
     *
     * @param query 查询条件
     * @return 任务定义分页结果
     */
    PageResult<MangoJobDefinitionVO> pageDefinitions(MangoJobDefinitionPageQuery query);

    /**
     * 查询任务定义详情。
     *
     * @param id 任务定义 ID
     * @return 任务定义详情
     */
    MangoJobDefinitionVO detailDefinition(Long id);

    /**
     * 创建任务定义。
     *
     * @param command 保存命令
     * @return 新任务定义 ID
     */
    Long createDefinition(SaveMangoJobDefinitionCommand command);

    /**
     * 更新任务定义。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    Boolean updateDefinition(SaveMangoJobDefinitionCommand command);

    /**
     * 更新任务启停状态。
     *
     * @param command 状态更新命令
     * @return true 表示更新成功
     */
    Boolean updateDefinitionStatus(UpdateMangoJobDefinitionStatusCommand command);

    /**
     * 删除任务定义。
     *
     * @param id 任务定义 ID
     * @return true 表示删除成功
     */
    Boolean deleteDefinition(Long id);

    /**
     * 手动触发任务。
     *
     * @param command 触发命令
     * @return 执行实例 ID
     */
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
