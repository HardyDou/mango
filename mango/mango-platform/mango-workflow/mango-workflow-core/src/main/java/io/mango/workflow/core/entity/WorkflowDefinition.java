package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义实体。
 */
@Data
@TableName("workflow_definition")
public class WorkflowDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long categoryId;
    private Long orgId;
    private String adminUsers;
    private String icon;
    private String definitionName;
    private String definitionKey;
    private String deploymentId;
    private String processDefinitionId;
    private Integer processDefinitionVersion;
    private Integer publishedVersionNo;
    private Long sourceTemplateId;
    private String sourceTemplateCode;
    private Integer sourceTemplateVersion;
    private String designerJson;
    private String bpmnXml;
    private String formCode;
    private String formJson;
    private String status;
    private LocalDateTime lastDeployTime;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
