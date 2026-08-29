package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowDesignerOptionsVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;

import java.util.List;

/**
 * 流程定义服务。
 */
public interface IWorkflowDefinitionService {

    PageResult<WorkflowDefinitionVO> page(WorkflowDefinitionPageQuery query);

    WorkflowDefinitionVO get(Long id);

    String create(SaveWorkflowDefinitionCommand command);

    Boolean update(SaveWorkflowDefinitionCommand command);

    Boolean delete(Long id);

    Boolean updateStatus(UpdateWorkflowDefinitionStatusCommand command);

    Boolean discardDraft(Long id);

    WorkflowDeployVO deploy(Long id);

    WorkflowDeployVO deployInternal(Long id);

    WorkflowDeployVO ensurePublished(EnsureWorkflowDefinitionCommand command);

    List<WorkflowDefinitionVersionVO> versions(WorkflowDefinitionVersionQuery query);

    WorkflowDefinitionVersionVO versionDetail(Long id);

    List<WorkflowNodeCatalogVO> nodeCatalog();

    WorkflowDesignerOptionsVO designerOptions();
}
