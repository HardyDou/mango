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

    PageResult<WorkflowTemplateCategoryVO> page(WorkflowTemplateCategoryPageQuery query);

    List<WorkflowTemplateCategoryVO> list(Integer status);

    WorkflowTemplateCategoryVO get(Long id);

    String create(SaveWorkflowTemplateCategoryCommand command);

    Boolean update(SaveWorkflowTemplateCategoryCommand command);

    Boolean delete(Long id);
}
