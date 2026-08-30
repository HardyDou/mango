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
import io.mango.workflow.api.vo.WorkflowTenantOptionVO;

import java.util.List;

/**
 * 流程模板服务。
 */
public interface IWorkflowTemplateService {

    PageResult<WorkflowTemplateVO> page(WorkflowTemplatePageQuery query);

    WorkflowTemplateVO get(Long id);

    String create(SaveWorkflowTemplateCommand command);

    Boolean delete(Long id);

    String createFromDefinition(CreateWorkflowTemplateFromDefinitionCommand command);

    String createDefinition(CreateWorkflowDefinitionFromTemplateCommand command);

    WorkflowTemplateImportVO importTemplates(ImportWorkflowTemplatesCommand command);

    WorkflowTemplateImportVO pushTemplates(PushWorkflowTemplatesCommand command);

    List<WorkflowTenantOptionVO> tenantOptions(String keyword);
}
