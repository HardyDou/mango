package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程模板实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_template")
public class WorkflowTemplateEntity extends WorkflowBaseEntity {

    private String templateName;
    private String templateCode;
    private Long templateCategoryId;
    private String categoryCode;
    private String categoryName;
    private String icon;
    private String adminUsers;
    private String designerJson;
    private String formCode;
    private String formJson;
    private Integer versionNo;
    private Boolean latestFlag;
    private String status;
    private Long sourceDefinitionId;
    private String sourceDefinitionKey;
    private String sourceDefinitionName;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
