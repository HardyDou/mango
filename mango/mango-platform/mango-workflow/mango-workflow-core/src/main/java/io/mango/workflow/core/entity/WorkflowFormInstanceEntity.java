package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程实例表单数据快照。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_form_instance")
public class WorkflowFormInstanceEntity extends WorkflowBaseEntity {

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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
