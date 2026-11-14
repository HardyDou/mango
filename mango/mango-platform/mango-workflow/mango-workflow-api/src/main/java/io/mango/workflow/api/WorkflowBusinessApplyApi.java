package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.UpdateWorkflowBusinessApplyFilterAttributesCommand;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyPageRequest;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.annotation.WorkflowBusinessDataPermission;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 业务工作流申请中心 API。
 */
public interface WorkflowBusinessApplyApi {

    R<WorkflowBusinessApplyVO> create(@Valid CreateWorkflowBusinessApplyCommand command);

    /**
     * 更新业务申请的可筛属性，用于版本状态这类提交后会变化的值。
     *
     * <p>整体覆盖：传入的键值替换该申请已登记的全部可筛属性，传空集合表示清空；重复调用结果一致。
     * 申请 ID 放在命令对象里，不使用路径参数，与平台 HTTP 适配规范一致。
     * 调用方必须是当前租户内的业务模块；平台只做租户隔离和申请存在性校验，具体数据权限由业务模块
     * 在调用前自行判定。
     *
     * @param command 申请 ID 与新的可筛属性集合
     * @return 更新结果；申请不存在或跨租户时返回 {@code APPLY_NOT_FOUND}
     */
    R<Void> updateFilterAttributes(@Valid UpdateWorkflowBusinessApplyFilterAttributesCommand command);

    R<PageResult<WorkflowBusinessApplyVO>> page(@Valid WorkflowBusinessApplyPageRequest request);

    R<WorkflowBusinessApplySummaryVO> mySummary();

    @WorkflowBusinessDataPermission(businessType = "*")
    R<WorkflowBusinessApplyVO> detail(@NotNull Long applyId);

    @WorkflowBusinessDataPermission(businessType = "*")
    R<PageResult<WorkflowBusinessApplyVO>> history(@Valid WorkflowBusinessApplyPageQuery query);

    @WorkflowBusinessDataPermission(businessType = "*")
    R<WorkflowBusinessApplyProgressVO> latestProgress(
            @NotBlank String businessType, @NotBlank String businessKey);

    R<WorkflowBusinessApplyProgressBatchVO> latestProgressBatch(
            @Valid WorkflowBusinessApplyProgressBatchRequest request);

    R<List<WorkflowBusinessApplyVO>> latestByBusinessKeys(
            @Valid WorkflowBusinessApplyProgressBatchRequest request);

    @WorkflowBusinessDataPermission(businessType = "*")
    R<WorkflowBusinessApplyVO> byProcessInstance(@NotBlank String processInstanceId);

    /**
     * Reads the application associated with a process instance for trusted internal event handling.
     *
     * <p>This contract does not accept caller-supplied tenant or owner coordinates. User-facing reads must keep using
     * {@link #byProcessInstance(String)} so that business data permission checks remain in force.
     *
     * @param processInstanceId process instance identifier
     * @return associated application, or {@code null} when no application is associated
     */
    R<WorkflowBusinessApplyVO> findByProcessInstance(@NotBlank String processInstanceId);
}
