package io.mango.workflow.api;

import io.mango.workflow.api.vo.WorkflowDesignerOptionsVO;

/**
 * 流程设计器候选数据扩展点。
 *
 * <p>实现方负责基于当前登录上下文提供当前租户和数据范围内的用户、角色、岗位、组织与字典类型。
 * Workflow 不接受客户端传入租户标识，也不会在 Provider 缺失或加载失败时返回伪造的空数据。
 */
public interface WorkflowDesignerOptionProvider {

    /**
     * 加载流程设计器候选数据。
     *
     * @return 非空候选数据集合。
     */
    WorkflowDesignerOptionsVO options();
}
