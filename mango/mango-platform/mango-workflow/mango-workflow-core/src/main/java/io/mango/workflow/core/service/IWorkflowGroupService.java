package io.mango.workflow.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowGroupCommand;
import io.mango.workflow.api.query.WorkflowGroupPageQuery;
import io.mango.workflow.api.vo.WorkflowGroupVO;

import java.util.List;

/**
 * 流程分组服务。
 */
public interface IWorkflowGroupService {

    R<PageResult<WorkflowGroupVO>> page(WorkflowGroupPageQuery query);

    R<List<WorkflowGroupVO>> list(Integer status);

    R<WorkflowGroupVO> get(Long id);

    R<Long> create(SaveWorkflowGroupCommand command);

    R<Boolean> update(SaveWorkflowGroupCommand command);

    R<Boolean> delete(Long id);
}
