package io.mango.workflow.api;

import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;

import java.util.Collection;
import java.util.List;

/**
 * 业务侧查询工作流实例状态的窄接口。
 */
public interface WorkflowBusinessProcessApi {

    /**
     * 按业务主键批量查询每个业务最新一次申请的流程状态。
     *
     * @param businessKeys 业务主键集合
     * @return 最新流程状态列表
     */
    List<WorkflowBusinessProcessVO> latestByBusinessKeys(Collection<String> businessKeys);

    /**
     * 按业务类型和业务主键批量查询每个业务最新一次申请的流程状态。
     *
     * @param businessType 业务类型
     * @param businessKeys 业务主键集合
     * @return 最新流程状态列表
     */
    List<WorkflowBusinessProcessVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys);
}
