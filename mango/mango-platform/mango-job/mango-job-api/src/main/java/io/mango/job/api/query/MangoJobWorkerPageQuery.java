package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job Worker 分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job Worker 分页查询")
public class MangoJobWorkerPageQuery extends PageQuery {

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "执行服务编码")
    private String serviceCode;

    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Schema(description = "通信方式")
    private String transportType;

    @Schema(description = "注册来源")
    private String registerSource;

    @Schema(description = "Worker 状态")
    private String status;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "关键词。支持 Worker 地址模糊搜索")
    private String keyword;
}
