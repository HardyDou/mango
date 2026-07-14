package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.SaveWorkflowTemplateCategoryCommand;
import io.mango.workflow.api.query.WorkflowTemplateCategoryPageQuery;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.mango.workflow.api.vo.WorkflowTemplateCategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 流程模板分类 API。 */
public interface WorkflowTemplateCategoryApi {

    R<PageResult<WorkflowTemplateCategoryVO>> page(@Valid WorkflowTemplateCategoryPageQuery query);

    R<List<WorkflowTemplateCategoryVO>> list(
            @NotNull(groups = WorkflowOptionalValidation.class) Integer status);

    R<WorkflowTemplateCategoryVO> get(@NotNull Long id);

    R<String> create(@Valid SaveWorkflowTemplateCategoryCommand command);

    R<Boolean> update(@Valid SaveWorkflowTemplateCategoryCommand command);

    R<Boolean> delete(@NotNull Long id);
}
