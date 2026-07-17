package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 日志索引分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 日志索引分页查询")
public class MangoJobLogPageQuery extends PageQuery {

    @Positive(message = "任务 ID 必须大于0")
    @Schema(description = "任务 ID")
    private Long jobId;

    @Positive(message = "实例 ID 必须大于0")
    @Schema(description = "实例 ID")
    private Long instanceId;

    @Size(max = 32, message = "引擎类型不能超过32个字符")
    @Schema(description = "引擎类型")
    private String engineType;
}
