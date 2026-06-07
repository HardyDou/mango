package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存 Mango Job 告警规则命令。
 */
@Data
@Schema(description = "保存 Mango Job 告警规则命令")
public class SaveMangoJobAlarmRuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "告警规则 ID。新增时为空，修改时必填")
    private Long id;

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

    @Schema(description = "触发条件 JSON。失败实例规则固定为 {\"status\":\"FAILED\"}")
    private String triggerCondition;

    @NotBlank(message = "通知场景编码不能为空")
    @Size(max = 128, message = "通知场景编码不能超过128个字符")
    @Schema(description = "通知场景编码，映射 mango-notice bizType", requiredMode = Schema.RequiredMode.REQUIRED)
    private String noticeSceneCode;

    @NotBlank(message = "通知模板编码不能为空")
    @Size(max = 128, message = "通知模板编码不能超过128个字符")
    @Schema(description = "通知模板编码，作为 noticeTemplateCode 参数传给 mango-notice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String noticeTemplateCode;

    @Schema(description = "通知参数 JSON。支持 userId、userIds、recipientRuleCode")
    private String noticeParams;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
