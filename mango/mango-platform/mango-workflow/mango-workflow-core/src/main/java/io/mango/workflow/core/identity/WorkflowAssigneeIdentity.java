package io.mango.workflow.core.identity;

/**
 * Workflow 办理人对应的 Mango 用户身份。
 *
 * @param userId Mango 用户 ID
 * @param displayName 昵称优先、用户名兜底的显示名
 */
public record WorkflowAssigneeIdentity(Long userId, String displayName) {
}
