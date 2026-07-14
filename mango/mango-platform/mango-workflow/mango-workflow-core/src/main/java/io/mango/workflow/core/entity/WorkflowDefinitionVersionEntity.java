package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程定义发布版本实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_definition_version")
public class WorkflowDefinitionVersionEntity extends WorkflowBaseEntity {

    private Long definitionId;
    private Integer versionNo;
    private Long categoryId;
    private String domainCode;
    private String adminUsers;
    private Boolean startEntryVisible;
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
    private LocalDateTime publishTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
