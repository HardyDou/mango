package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程节点定义实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node_definition")
public class WorkflowNodeDefinitionEntity extends WorkflowBaseEntity {

    private String nodeDefinitionCode;
    private String nodeType;
    private String nodeName;
    private String categoryCode;
    private String categoryName;
    private String description;
    private String bpmnType;
    private String executionType;
    private String color;
    private String icon;
    private String propertySchema;
    private String defaultProperties;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
