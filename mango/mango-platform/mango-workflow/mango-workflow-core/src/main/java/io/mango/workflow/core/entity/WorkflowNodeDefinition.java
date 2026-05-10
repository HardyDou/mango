package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点定义实体。
 */
@Data
@TableName("workflow_node_definition")
public class WorkflowNodeDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
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
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
