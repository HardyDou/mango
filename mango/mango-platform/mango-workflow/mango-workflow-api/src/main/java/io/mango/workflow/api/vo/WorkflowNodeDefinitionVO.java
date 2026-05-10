package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点定义视图。
 */
@Data
@Schema(description = "流程节点定义视图")
public class WorkflowNodeDefinitionVO {

    @Schema(description = "节点定义ID")
    private Long id;

    @Schema(description = "节点定义编码")
    private String nodeDefinitionCode;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点分类编码")
    private String categoryCode;

    @Schema(description = "节点分类名称")
    private String categoryName;

    @Schema(description = "节点说明")
    private String description;

    @Schema(description = "底层BPMN类型")
    private String bpmnType;

    @Schema(description = "执行类型")
    private String executionType;

    @Schema(description = "节点颜色")
    private String color;

    @Schema(description = "节点图标")
    private String icon;

    @Schema(description = "属性配置JSON Schema")
    private String propertySchema;

    @Schema(description = "默认属性JSON")
    private String defaultProperties;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
