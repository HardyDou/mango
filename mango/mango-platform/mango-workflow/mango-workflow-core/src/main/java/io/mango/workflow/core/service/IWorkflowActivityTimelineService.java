package io.mango.workflow.core.service;

import io.mango.workflow.api.query.WorkflowActivityTimelineQuery;
import io.mango.workflow.api.vo.WorkflowActivityTimelineVO;

import java.util.List;

/** 活动级时间轴的读取入口：数据来自流程引擎历史活动表，与任务流转记录互补。 */
public interface IWorkflowActivityTimelineService {

    /**
     * 读取业务键下全部流程实例的活动时间轴。
     *
     * @param request 查询条件，业务类型与业务键必填
     * @return 活动行列表；业务键未登记流程实例时返回空集合
     */
    List<WorkflowActivityTimelineVO> activityTimelineByBusinessKey(WorkflowActivityTimelineQuery request);
}
