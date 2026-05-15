package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例表单数据快照。
 */
@Data
@TableName("workflow_form_instance")
public class WorkflowFormInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String processInstanceId;
    private String businessKey;
    private Long definitionId;
    private String definitionKey;
    private String definitionName;
    private String processDefinitionId;
    private Integer processDefinitionVersion;
    private String formCode;
    private String formJson;
    private String variablesJson;
    private String status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
