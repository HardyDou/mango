package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 告警规则返回对象。
 */
@Data
@Schema(description = "Job 告警规则返回对象")
public class MangoJobAlarmRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "告警规则 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "任务定义 ID")
    private Long jobId;

    @Schema(description = "任务编码")
    private String jobCode;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "触发条件 JSON")
    private String triggerCondition;

    @Schema(description = "通知场景编码")
    private String noticeSceneCode;

    @Schema(description = "通知模板编码")
    private String noticeTemplateCode;

    @Schema(description = "通知参数 JSON")
    private String noticeParams;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建人 ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人 ID")
    private Long updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
