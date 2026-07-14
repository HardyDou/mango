package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流抄送待阅记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_copied_task")
public class WorkflowCopiedTaskEntity extends WorkflowBaseEntity {

    private String processInstanceId;
    private String processDefinitionId;
    private String processName;
    private String processKey;
    private String businessKey;
    private String nodeDefinitionKey;
    private String nodeName;
    private String copiedUserId;
    private String copiedUserName;
    private String message;
    private Boolean readFlag;
    private LocalDateTime readTime;
    private LocalDateTime createdTime;
}
