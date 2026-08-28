package io.mango.workflow.core.identity;

import java.util.Collection;
import java.util.Map;

/**
 * Workflow 办理人身份批量解析扩展点。
 */
public interface IWorkflowAssigneeIdentityProvider {

    /**
     * 在当前租户上下文中批量解析原始 Flowable 办理人键。
     *
     * @param assigneeKeys 去重后的原始办理人键
     * @return 以原始办理人键为 key 的已解析身份；未解析项不返回
     */
    Map<String, WorkflowAssigneeIdentity> resolveAll(Collection<String> assigneeKeys);
}
