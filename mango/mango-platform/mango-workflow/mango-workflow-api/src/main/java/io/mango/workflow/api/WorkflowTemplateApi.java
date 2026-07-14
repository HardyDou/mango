package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowDefinitionFromTemplateCommand;
import io.mango.workflow.api.command.CreateWorkflowTemplateFromDefinitionCommand;
import io.mango.workflow.api.command.ImportWorkflowTemplatesCommand;
import io.mango.workflow.api.command.PushWorkflowTemplatesCommand;
import io.mango.workflow.api.command.SaveWorkflowTemplateCommand;
import io.mango.workflow.api.query.WorkflowTemplatePageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateImportVO;
import io.mango.workflow.api.vo.WorkflowTemplateVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 流程模板 API。 */
public interface WorkflowTemplateApi {

    R<PageResult<WorkflowTemplateVO>> page(@Valid WorkflowTemplatePageQuery query);

    R<WorkflowTemplateVO> get(@NotNull Long id);

    R<String> create(@Valid SaveWorkflowTemplateCommand command);

    R<Boolean> delete(@NotNull Long id);

    R<String> createFromDefinition(@Valid CreateWorkflowTemplateFromDefinitionCommand command);

    R<String> createDefinition(@Valid CreateWorkflowDefinitionFromTemplateCommand command);

    R<WorkflowTemplateImportVO> importTemplates(@Valid ImportWorkflowTemplatesCommand command);

    R<WorkflowTemplateImportVO> pushTemplates(@Valid PushWorkflowTemplatesCommand command);
}
