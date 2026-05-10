package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存流程节点定义命令。
 */
@Data
@Schema(description = "保存流程节点定义命令")
public class SaveWorkflowNodeDefinitionCommand {

    @Schema(description = "节点定义ID，新增时为空，修改时必填")
    private Long id;

    @Schema(description = "节点定义编码，全局唯一")
    @NotBlank(message = "节点定义编码不能为空")
    @Size(max = 64, message = "节点定义编码最多64个字符")
    private String nodeDefinitionCode;

    @Schema(description = "节点类型，用于设计器和后端转换识别")
    @NotBlank(message = "节点类型不能为空")
    @Size(max = 64, message = "节点类型最多64个字符")
    private String nodeType;

    @Schema(description = "节点名称")
    @NotBlank(message = "节点名称不能为空")
    @Size(max = 64, message = "节点名称最多64个字符")
    private String nodeName;

    @Schema(description = "节点分类编码")
    @NotBlank(message = "节点分类编码不能为空")
    @Size(max = 64, message = "节点分类编码最多64个字符")
    private String categoryCode;

    @Schema(description = "节点分类名称")
    @NotBlank(message = "节点分类名称不能为空")
    @Size(max = 64, message = "节点分类名称最多64个字符")
    private String categoryName;

    @Schema(description = "节点说明")
    @Size(max = 255, message = "节点说明最多255个字符")
    private String description;

    @Schema(description = "底层BPMN类型：startEvent、userTask、serviceTask、exclusiveGateway、parallelGateway")
    @NotBlank(message = "底层BPMN类型不能为空")
    @Size(max = 64, message = "底层BPMN类型最多64个字符")
    private String bpmnType;

    @Schema(description = "执行类型：NONE、USER_TASK、SPRING_BEAN、HTTP_URL、REMOTE_SERVICE、EVENT_PUBLISH")
    @NotBlank(message = "执行类型不能为空")
    @Size(max = 64, message = "执行类型最多64个字符")
    private String executionType;

    @Schema(description = "节点颜色")
    @Size(max = 32, message = "节点颜色最多32个字符")
    private String color;

    @Schema(description = "节点图标")
    @Size(max = 64, message = "节点图标最多64个字符")
    private String icon;

    @Schema(description = "属性配置JSON Schema")
    private String propertySchema;

    @Schema(description = "默认属性JSON")
    private String defaultProperties;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：0-停用，1-启用")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
