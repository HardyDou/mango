package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 任务定义分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 任务定义分页查询")
public class MangoJobDefinitionPageQuery extends PageQuery {

    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Size(max = 128, message = "执行服务编码不能超过128个字符")
    @Schema(description = "执行服务编码")
    private String ownerService;

    @Size(max = 128, message = "Worker 分组不能超过128个字符")
    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Size(max = 32, message = "任务状态不能超过32个字符")
    @Schema(description = "任务状态")
    private String status;

    @Size(max = 32, message = "任务类型不能超过32个字符")
    @Schema(description = "任务类型")
    private String jobType;

    @Size(max = 32, message = "调度类型不能超过32个字符")
    @Schema(description = "调度类型")
    private String scheduleType;

    @Size(max = 32, message = "引擎类型不能超过32个字符")
    @Schema(description = "引擎类型")
    private String engineType;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词。支持任务编码、任务名称、处理器名称模糊搜索")
    private String keyword;
}
