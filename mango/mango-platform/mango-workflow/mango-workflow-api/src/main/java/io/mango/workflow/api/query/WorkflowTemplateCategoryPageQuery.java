package io.mango.workflow.api.query;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模板分类分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程模板分类分页查询")
public class WorkflowTemplateCategoryPageQuery extends PageQuery {

    @Schema(description = "关键字，支持按分类名称或编码模糊查询")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String keyword;

    @Schema(description = "状态：0-停用，1-启用")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Integer status;
}
