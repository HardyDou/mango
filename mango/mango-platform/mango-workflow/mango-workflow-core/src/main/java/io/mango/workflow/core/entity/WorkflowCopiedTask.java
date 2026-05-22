package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流抄送待阅记录。
 */
@Data
@TableName("workflow_copied_task")
public class WorkflowCopiedTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
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
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
