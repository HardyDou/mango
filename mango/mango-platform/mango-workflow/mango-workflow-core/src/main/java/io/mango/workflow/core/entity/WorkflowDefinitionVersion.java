package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义发布版本实体。
 */
@Data
@TableName("workflow_definition_version")
public class WorkflowDefinitionVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long definitionId;
    private Integer versionNo;
    private Long categoryId;
    private String domainCode;
    private Long orgId;
    private String adminUsers;
    private String icon;
    private String definitionName;
    private String definitionKey;
    private String remark;
    private String formCode;
    private String designerJson;
    private String formJson;
    private String bpmnXml;
    private String deploymentId;
    private String processDefinitionId;
    private Integer processDefinitionVersion;
    private String publishStatus;
    private String publishMessage;
    private Long createdBy;
    private LocalDateTime publishTime;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
