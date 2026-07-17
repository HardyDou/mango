package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 告警规则分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 告警规则分页查询")
public class MangoJobAlarmRulePageQuery extends PageQuery {

    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Positive(message = "任务定义 ID 必须大于0")
    @Schema(description = "任务定义 ID")
    private Long jobId;

    @Size(max = 64, message = "告警类型不能超过64个字符")
    @Schema(description = "告警类型")
    private String alarmType;

    @Pattern(regexp = "true|false", message = "是否启用只能为 true 或 false")
    @Schema(description = "是否启用")
    private String enabled;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词。支持规则名称、通知场景编码、通知模板编码模糊搜索")
    private String keyword;
}
