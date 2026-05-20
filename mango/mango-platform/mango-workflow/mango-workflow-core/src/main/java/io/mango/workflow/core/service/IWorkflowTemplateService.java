package io.mango.workflow.core.service;

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

/**
 * 流程模板服务。
 */
public interface IWorkflowTemplateService {

    R<PageResult<WorkflowTemplateVO>> page(WorkflowTemplatePageQuery query);

    R<WorkflowTemplateVO> get(Long id);

    R<String> create(SaveWorkflowTemplateCommand command);

    R<Boolean> delete(Long id);

    R<String> createFromDefinition(CreateWorkflowTemplateFromDefinitionCommand command);

    R<String> createDefinition(CreateWorkflowDefinitionFromTemplateCommand command);

    R<WorkflowTemplateImportVO> importTemplates(ImportWorkflowTemplatesCommand command);

    R<WorkflowTemplateImportVO> pushTemplates(PushWorkflowTemplatesCommand command);
}
