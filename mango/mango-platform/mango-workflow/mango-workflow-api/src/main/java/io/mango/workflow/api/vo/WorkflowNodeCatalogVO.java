package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流设计器节点目录视图。
 */
@Data
@Schema(description = "工作流设计器节点目录视图")
public class WorkflowNodeCatalogVO {

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

    @Schema(description = "节点分组")
    private String groupName;

    @Schema(description = "节点说明")
    private String description;

    @Schema(description = "底层BPMN类型")
    private String bpmnType;

    @Schema(description = "执行类型：NONE-无执行动作，USER_TASK-人工任务，SPRING_BEAN-Spring Bean，HTTP_URL-HTTP URL，REMOTE_SERVICE-远程服务，EVENT_PUBLISH-事件发布")
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

    public static WorkflowNodeCatalogVO of(String nodeDefinitionCode, String nodeType, String nodeName,
                                           String categoryCode, String categoryName, String groupName,
                                           String description, String bpmnType, String executionType,
                                           String color, String icon, String propertySchema,
                                           String defaultProperties, Integer sort) {
        WorkflowNodeCatalogVO vo = new WorkflowNodeCatalogVO();
        vo.setNodeDefinitionCode(nodeDefinitionCode);
        vo.setNodeType(nodeType);
        vo.setNodeName(nodeName);
        vo.setCategoryCode(categoryCode);
        vo.setCategoryName(categoryName);
        vo.setGroupName(groupName);
        vo.setDescription(description);
        vo.setBpmnType(bpmnType);
        vo.setExecutionType(executionType);
        vo.setColor(color);
        vo.setIcon(icon);
        vo.setPropertySchema(propertySchema);
        vo.setDefaultProperties(defaultProperties);
        vo.setSort(sort);
        return vo;
    }
}
