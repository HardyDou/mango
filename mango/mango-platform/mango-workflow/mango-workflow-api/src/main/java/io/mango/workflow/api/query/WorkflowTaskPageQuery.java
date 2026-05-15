package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 协同办公任务分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "协同办公任务分页查询")
public class WorkflowTaskPageQuery extends PageQuery {

    @Schema(description = "关键字，支持按流程名称、任务名称搜索")
    private String keyword;
}
