package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 任务定义分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 任务定义分页查询")
public class MangoJobDefinitionPageQuery extends PageQuery {

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "执行服务编码")
    private String ownerService;

    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "任务类型")
    private String jobType;

    @Schema(description = "调度类型")
    private String scheduleType;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "关键词。支持任务编码、任务名称、处理器名称模糊搜索")
    private String keyword;
}
