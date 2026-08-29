package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批中心任务视图。
 */
@Data
@Schema(description = "审批中心任务视图")
public class WorkflowTaskVO {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务定义Key，对应流程设计器节点定义Key")
    private String taskDefinitionKey;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程编码")
    private String processKey;

    @Schema(description = "Flowable流程定义ID")
    private String processDefinitionId;

    @Schema(description = "发起人")
    private String initiatorName;

    @Schema(description = "Flowable 原始办理人 key")
    private String assigneeName;

    @Schema(description = "办理人 Mango 用户 ID；未认领或无法解析时为空")
    private Long assigneeId;

    @Schema(description = "办理人显示名；昵称优先、用户名兜底，无法解析时为空")
    private String assigneeDisplayName;

    @Schema(description = "认领状态")
    private WorkflowTaskClaimStatus claimStatus;

    @Schema(description = "候选用户")
    private List<String> candidateUsers;

    @Schema(description = "候选组")
    private List<String> candidateGroups;

    @Schema(description = "当前用户是否可认领")
    private Boolean claimable;

    @Schema(description = "当前用户是否可释放")
    private Boolean unclaimable;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    public List<String> getCandidateUsers() {
        return candidateUsers == null ? null : new ArrayList<>(candidateUsers);
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers == null ? null : new ArrayList<>(candidateUsers);
    }

    public List<String> getCandidateGroups() {
        return candidateGroups == null ? null : new ArrayList<>(candidateGroups);
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups == null ? null : new ArrayList<>(candidateGroups);
    }
}
