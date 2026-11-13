package io.mango.workflow.core.convert;

import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.vo.WorkflowJsonVO;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.core.entity.WorkflowBusinessApplyStatusLogEntity;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 任务处理记录到协议对象的转换。
 *
 * <p>处理变量的取值由调用方决定：详情类接口传入解析后的变量，流转记录读取接口传入 {@code null}，
 * 避免把每条记录的完整变量（含表单明文）透出给只展示动作和意见的场景。
 */
public final class WorkflowTaskRecordConverter {

    private WorkflowTaskRecordConverter() {
    }

    /**
     * 转换一条任务处理记录。
     *
     * @param record    处理记录，调用方必须保证非空
     * @param variables 需要随记录返回的处理变量；{@code null} 表示不返回变量
     * @return 处理记录视图
     */
    public static WorkflowTaskRecordVO toVo(WorkflowTaskRecordEntity record, Map<String, Object> variables) {
        WorkflowTaskRecordVO vo = new WorkflowTaskRecordVO();
        vo.setId(record.getId());
        vo.setProcessInstanceId(record.getProcessInstanceId());
        vo.setTaskId(record.getTaskId());
        vo.setTaskName(record.getTaskName());
        vo.setTaskDefinitionKey(record.getTaskDefinitionKey());
        vo.setAction(record.getAction());
        vo.setActionName(StringUtils.hasText(record.getActionName())
                ? record.getActionName()
                : WorkflowTaskAction.labelOf(record.getAction()));
        vo.setOperatorId(record.getOperatorId());
        vo.setOperatorName(record.getOperatorName());
        vo.setComment(record.getComment());
        vo.setVariables(variables == null ? null : WorkflowJsonVO.of(variables));
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }


    /**
     * 把申请状态流水中的撤回记录转换成流转记录。
     *
     * <p>撤回不写 {@code workflow_task_record}，只在 {@code workflow_business_apply_status_log} 留痕；
     * 为让时间轴完整（含升级前已撤回的历史申请），读取时并入同一 VO，动作固定为
     * {@link WorkflowTaskAction#WITHDRAW}。
     *
     * @param log 状态流水，调用方必须保证动作为撤回
     * @return 处理记录视图；处理变量恒为 null
     */
    public static WorkflowTaskRecordVO toVo(WorkflowBusinessApplyStatusLogEntity log) {
        WorkflowTaskRecordVO vo = new WorkflowTaskRecordVO();
        vo.setId(log.getId());
        vo.setProcessInstanceId(log.getProcessInstanceId());
        vo.setTaskId(log.getTaskId());
        vo.setTaskDefinitionKey(log.getTaskDefinitionKey());
        vo.setAction(WorkflowTaskAction.WITHDRAW.name());
        vo.setActionName(StringUtils.hasText(log.getActionName())
                ? log.getActionName()
                : WorkflowTaskAction.WITHDRAW.getLabel());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setComment(log.getComment());
        vo.setCreatedTime(log.getCreatedAt());
        return vo;
    }
}
