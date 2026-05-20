package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowTemplateCategoryCommand;
import io.mango.workflow.api.query.WorkflowTemplateCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateCategoryVO;

import java.util.List;

/**
 * 流程模板分类服务。
 */
public interface IWorkflowTemplateCategoryService {

    R<PageResult<WorkflowTemplateCategoryVO>> page(WorkflowTemplateCategoryPageQuery query);

    R<List<WorkflowTemplateCategoryVO>> list(Integer status);

    R<WorkflowTemplateCategoryVO> get(Long id);

    R<String> create(SaveWorkflowTemplateCategoryCommand command);

    R<Boolean> update(SaveWorkflowTemplateCategoryCommand command);

    R<Boolean> delete(Long id);
}
