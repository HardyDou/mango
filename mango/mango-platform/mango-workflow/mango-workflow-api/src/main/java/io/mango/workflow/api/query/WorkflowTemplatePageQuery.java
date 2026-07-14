package io.mango.workflow.api.query;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模板分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程模板分页查询")
public class WorkflowTemplatePageQuery extends PageQuery {

    @Schema(description = "关键字，支持按模板名称、模板编码或业务场景模糊查询")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String keyword;

    @Schema(description = "模板状态：ENABLED-启用，DISABLED-停用")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String status;

    @Schema(description = "流程模板分类ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long templateCategoryId;

    @Schema(description = "业务场景编码")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String categoryCode;
}
