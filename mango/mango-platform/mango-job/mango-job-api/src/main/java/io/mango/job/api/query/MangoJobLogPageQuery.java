package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 日志索引分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 日志索引分页查询")
public class MangoJobLogPageQuery extends PageQuery {

    @Schema(description = "任务 ID")
    private Long jobId;

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "引擎类型")
    private String engineType;
}
