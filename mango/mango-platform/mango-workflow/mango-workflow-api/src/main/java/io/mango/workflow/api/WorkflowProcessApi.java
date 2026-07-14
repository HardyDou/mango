package io.mango.workflow.api;

import io.mango.common.result.R;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
import io.mango.common.vo.PageResult;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 流程实例 API。
 */
public interface WorkflowProcessApi {

    /**
     * 发起流程。
     *
     * @param command 发起流程命令
     * @return 流程实例
     */
    R<WorkflowProcessInstanceVO> start(@Valid StartWorkflowProcessCommand command);

    /**
     * 创建业务申请并发起流程，返回业务可直接展示的当前进度快照。
     *
     * @param command 业务流程一体化启动命令
     * @return 启动结果
     */
    R<WorkflowStartResultVO> startBusinessWorkflow(@Valid StartBusinessWorkflowCommand command);

    R<PageResult<WorkflowProcessInstanceVO>> initiated(@Valid WorkflowTaskPageQuery query);

    R<WorkflowProcessDetailVO> detail(@NotBlank String processInstanceId);

    R<PageResult<WorkflowProcessInstanceVO>> history(
            @NotBlank String businessKey, @Valid WorkflowTaskPageQuery query);
}
