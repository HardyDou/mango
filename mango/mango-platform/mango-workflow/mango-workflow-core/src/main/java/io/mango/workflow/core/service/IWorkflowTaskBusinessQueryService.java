package io.mango.workflow.core.service;

import io.mango.common.vo.PageResult;
import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.mango.workflow.api.vo.WorkflowTaskVO;

/**
 * 按业务类型查询任务，业务主键可选。
 */
public interface IWorkflowTaskBusinessQueryService {

    /**
     * 按业务类型查询全部任务。
     *
     * <p>返回范围包含在途、已办理和已终止任务；租户隔离始终生效。
     * {@code businessKeys} 不传或为空表示不限业务主键；传入时业务申请可见性参与收窄。
     * 不传分页参数时按配置上限一次性返回，调用方通过 {@code total} 与列表长度判断是否需要改用分页拉取。
     *
     * @param request 任务查询条件
     * @return 任务分页结果
     */
    PageResult<WorkflowTaskVO> pageByBusinessKey(WorkflowTaskBusinessPageQuery request);
}
