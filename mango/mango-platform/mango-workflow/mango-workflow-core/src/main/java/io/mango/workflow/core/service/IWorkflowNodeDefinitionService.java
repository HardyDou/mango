package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowNodeDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowNodeDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowNodeDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowNodeDefinitionVO;

import java.util.List;

/**
 * 流程节点定义服务。
 */
public interface IWorkflowNodeDefinitionService {

    R<PageResult<WorkflowNodeDefinitionVO>> page(WorkflowNodeDefinitionPageQuery query);

    R<List<WorkflowNodeDefinitionVO>> list(Integer status);

    R<WorkflowNodeDefinitionVO> get(Long id);

    R<Long> create(SaveWorkflowNodeDefinitionCommand command);

    R<Boolean> update(SaveWorkflowNodeDefinitionCommand command);

    R<Boolean> updateStatus(UpdateWorkflowNodeDefinitionStatusCommand command);

    R<Boolean> delete(Long id);
}
