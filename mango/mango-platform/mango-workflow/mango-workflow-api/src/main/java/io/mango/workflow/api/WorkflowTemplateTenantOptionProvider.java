package io.mango.workflow.api;

import io.mango.workflow.api.vo.WorkflowTenantOptionVO;

import java.util.List;

/** 流程模板推送目标机构候选数据 Provider。 */
public interface WorkflowTemplateTenantOptionProvider {

    /** 查询当前操作者可用于模板推送的启用机构。 */
    List<WorkflowTenantOptionVO> options(String keyword);
}
