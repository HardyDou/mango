package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模板实体。
 */
@Data
@TableName("workflow_template")
public class WorkflowTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
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
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
