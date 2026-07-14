package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 流程定义 API。
 */
public interface WorkflowDefinitionApi {

    R<PageResult<WorkflowDefinitionVO>> page(@Valid WorkflowDefinitionPageQuery query);

    R<WorkflowDefinitionVO> get(@NotNull Long id);

    R<String> create(@Valid SaveWorkflowDefinitionCommand command);

    R<Boolean> update(@Valid SaveWorkflowDefinitionCommand command);

    R<Boolean> delete(@NotNull Long id);

    R<Boolean> updateStatus(@Valid UpdateWorkflowDefinitionStatusCommand command);

    R<Boolean> discardDraft(@NotNull Long id);

    R<WorkflowDeployVO> deploy(@NotNull Long id);

    /**
     * 确保流程定义存在并处于已发布可发起状态。
     *
     * @param command 流程定义初始化命令
     * @return 流程发布结果
     */
    R<WorkflowDeployVO> ensurePublished(@Valid EnsureWorkflowDefinitionCommand command);

    R<List<WorkflowDefinitionVersionVO>> versions(@Valid WorkflowDefinitionVersionQuery query);

    R<WorkflowDefinitionVersionVO> versionDetail(@NotNull Long id);

    R<List<WorkflowNodeCatalogVO>> nodeCatalog();
}
