package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 告警规则分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 告警规则分页查询")
public class MangoJobAlarmRulePageQuery extends PageQuery {

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "任务定义 ID")
    private Long jobId;

    @Schema(description = "告警类型")
    private String alarmType;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "关键词。支持规则名称、通知场景编码、通知模板编码模糊搜索")
    private String keyword;
}
