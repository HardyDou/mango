package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;

import java.util.List;

/**
 * 流程定义服务。
 */
public interface IWorkflowDefinitionService extends WorkflowDefinitionApi {

    R<PageResult<WorkflowDefinitionVO>> page(WorkflowDefinitionPageQuery query);

    R<WorkflowDefinitionVO> get(Long id);

    R<String> create(SaveWorkflowDefinitionCommand command);

    R<Boolean> update(SaveWorkflowDefinitionCommand command);

    R<Boolean> delete(Long id);

    R<Boolean> updateStatus(UpdateWorkflowDefinitionStatusCommand command);

    R<Boolean> discardDraft(Long id);

    R<WorkflowDeployVO> deploy(Long id);

    @Override
    R<WorkflowDeployVO> ensurePublished(EnsureWorkflowDefinitionCommand command);

    R<List<WorkflowDefinitionVersionVO>> versions(WorkflowDefinitionVersionQuery query);

    R<WorkflowDefinitionVersionVO> versionDetail(Long id);

    R<List<WorkflowNodeCatalogVO>> nodeCatalog();
}
