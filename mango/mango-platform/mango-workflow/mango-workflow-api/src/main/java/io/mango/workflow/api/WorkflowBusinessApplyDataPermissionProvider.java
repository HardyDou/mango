package io.mango.workflow.api;

import io.mango.workflow.api.vo.WorkflowBusinessApplyAccessVO;

/**
 * 业务模块提供的 Workflow 业务申请数据权限扩展点。
 * <p>
 * Provider 必须在自己的业务表上执行 owner、组织和租户校验；Workflow 只提供稳定的
 * 业务申请上下文，不直接访问业务模块表，也不接受调用方传入的租户或 owner 作为权威来源。
 */
public interface WorkflowBusinessApplyDataPermissionProvider {

    /**
     * 判断此 Provider 是否负责指定业务类型。
     *
     * @param businessType 业务类型。
     * @return 是否负责。
     */
    boolean supports(String businessType);

    /**
     * 校验当前主体是否可以读取该业务申请关联的 Workflow 数据。
     *
     * @param context 由 Workflow 数据库事实构造的上下文。
     * @return 是否允许读取。
     */
    boolean canRead(WorkflowBusinessApplyAccessVO context);
}
