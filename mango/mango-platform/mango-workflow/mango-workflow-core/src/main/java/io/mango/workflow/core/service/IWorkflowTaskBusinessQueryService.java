package io.mango.workflow.core.service;

import io.mango.common.vo.PageResult;
import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.mango.workflow.api.vo.WorkflowTaskVO;

/**
 * 按业务主键批量查询任务。
 */
public interface IWorkflowTaskBusinessQueryService {

    /**
     * 按业务主键批量查询全部任务。
     *
     * <p>返回范围包含在途、已办理和已终止任务；租户隔离与业务申请可见性始终生效。
     * 不传分页参数时按配置上限一次性返回，调用方通过 {@code total} 与列表长度判断是否需要改用分页拉取。
     *
     * @param query 业务主键批量查询条件
     * @return 任务分页结果
     */
    PageResult<WorkflowTaskVO> pageByBusinessKey(WorkflowTaskBusinessPageQuery query);
}
