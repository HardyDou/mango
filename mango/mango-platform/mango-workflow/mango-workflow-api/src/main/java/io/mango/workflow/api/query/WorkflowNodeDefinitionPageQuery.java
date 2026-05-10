package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程节点定义分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程节点定义分页查询")
public class WorkflowNodeDefinitionPageQuery extends PageQuery {

    @Schema(description = "关键字，支持按节点名称、节点定义编码或节点类型模糊查询")
    private String keyword;

    @Schema(description = "节点分类编码")
    private String categoryCode;

    @Schema(description = "底层BPMN类型")
    private String bpmnType;

    @Schema(description = "执行类型")
    private String executionType;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
