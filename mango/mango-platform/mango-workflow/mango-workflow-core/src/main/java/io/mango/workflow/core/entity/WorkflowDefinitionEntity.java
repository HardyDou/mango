package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程定义实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_definition")
public class WorkflowDefinitionEntity extends WorkflowBaseEntity {

    private Long categoryId;
    private String domainCode;
    private String adminUsers;
    private Boolean startEntryVisible;
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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
