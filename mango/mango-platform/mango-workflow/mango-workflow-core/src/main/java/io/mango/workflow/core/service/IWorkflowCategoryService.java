package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowCategoryCommand;
import io.mango.workflow.api.query.WorkflowCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowCategoryVO;

import java.util.List;

/**
 * 流程分类服务。
 */
public interface IWorkflowCategoryService {

    R<PageResult<WorkflowCategoryVO>> page(WorkflowCategoryPageQuery query);

    R<List<WorkflowCategoryVO>> list(Integer status);

    R<WorkflowCategoryVO> get(Long id);

    R<String> create(SaveWorkflowCategoryCommand command);

    R<Boolean> update(SaveWorkflowCategoryCommand command);

    R<Boolean> delete(Long id);
}
