package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建 Mango Job 告警规则命令。
 */
@Data
@Schema(description = "创建 Mango Job 告警规则命令")
public class CreateMangoJobAlarmRuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive(message = "任务定义 ID 必须大于0")
    @Schema(description = "任务定义 ID。为空表示应用级默认规则")
    private Long jobId;

    @NotBlank(message = "所属应用不能为空")
    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称不能超过128个字符")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    @NotBlank(message = "告警类型不能为空")
    @Size(max = 64, message = "告警类型不能超过64个字符")
    @Schema(description = "告警类型：INSTANCE_FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alarmType;

    @Size(max = 65535, message = "触发条件 JSON 不能超过65535个字符")
    @Schema(description = "触发条件 JSON。失败实例规则固定为 {\"status\":\"FAILED\"}")
    private String triggerCondition;

    @NotBlank(message = "通知场景编码不能为空")
    @Size(max = 128, message = "通知场景编码不能超过128个字符")
    @Schema(description = "通知场景编码。失败实例固定为 mango-notice 业务 Key：job.instance.failed",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String noticeSceneCode;

    @NotBlank(message = "通知模板编码不能为空")
    @Size(max = 128, message = "通知模板编码不能超过128个字符")
    @Schema(description = "通知模板编码，作为 noticeTemplateCode 参数传给 mango-notice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String noticeTemplateCode;

    @Size(max = 65535, message = "通知参数 JSON 不能超过65535个字符")
    @Schema(description = "通知参数 JSON。支持 userId、userIds、recipientRuleCode")
    private String noticeParams;

    @jakarta.validation.constraints.NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled = Boolean.TRUE;
}
