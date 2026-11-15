package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务可选办理人视图。
 *
 * <p>用于转办、改派时选择目标办理人。数据来源是节点配置在当前时点解析出的候选成员，
 * 与 Flowable 身份链无关：平台自动派单的任务不写候选身份链，靠身份链无法回答「能转给谁」。
 */
@Data
@Schema(description = "任务可选办理人视图")
public class WorkflowTaskAssigneeVO {

    @Schema(description = "办理人 Mango 用户 ID")
    private Long userId;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "办理人显示名；昵称优先、用户名兜底")
    private String displayName;

    @Schema(description = "是否任务当前承办人")
    private Boolean current;
}
