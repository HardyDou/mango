package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowCategoryCommand;
import io.mango.workflow.api.query.WorkflowCategoryPageQuery;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.mango.workflow.api.vo.WorkflowCategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 流程分类 API。 */
public interface WorkflowCategoryApi {

    R<PageResult<WorkflowCategoryVO>> page(@Valid WorkflowCategoryPageQuery query);

    R<List<WorkflowCategoryVO>> list(
            @NotNull(groups = WorkflowOptionalValidation.class) Integer status,
            @NotNull(groups = WorkflowOptionalValidation.class) String domainCode);

    R<WorkflowCategoryVO> get(@NotNull Long id);

    R<String> create(@Valid SaveWorkflowCategoryCommand command);

    R<Boolean> update(@Valid SaveWorkflowCategoryCommand command);

    R<Boolean> delete(@NotNull Long id);
}
