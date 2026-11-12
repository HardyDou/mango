package io.mango.workflow.core.model;

import lombok.Data;

/**
 * 任务候选身份关联行。
 */
@Data
public class WorkflowTaskIdentityLinkRow {

    private String taskId;
    private String userId;
    private String groupId;
}
