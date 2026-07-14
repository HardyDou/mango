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

    PageResult<WorkflowCategoryVO> page(WorkflowCategoryPageQuery query);

    List<WorkflowCategoryVO> list(Integer status, String domainCode);

    WorkflowCategoryVO get(Long id);

    String create(SaveWorkflowCategoryCommand command);

    Boolean update(SaveWorkflowCategoryCommand command);

    Boolean delete(Long id);
}
