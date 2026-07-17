package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job Worker 分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job Worker 分页查询")
public class MangoJobWorkerPageQuery extends PageQuery {

    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Size(max = 128, message = "执行服务编码不能超过128个字符")
    @Schema(description = "执行服务编码")
    private String serviceCode;

    @Size(max = 128, message = "Worker 分组不能超过128个字符")
    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Size(max = 32, message = "通信方式不能超过32个字符")
    @Schema(description = "通信方式")
    private String transportType;

    @Size(max = 32, message = "注册来源不能超过32个字符")
    @Schema(description = "注册来源")
    private String registerSource;

    @Size(max = 32, message = "Worker 状态不能超过32个字符")
    @Schema(description = "Worker 状态")
    private String status;

    @Size(max = 32, message = "引擎类型不能超过32个字符")
    @Schema(description = "引擎类型")
    private String engineType;

    @Size(max = 256, message = "关键词不能超过256个字符")
    @Schema(description = "关键词。支持 Worker 地址模糊搜索")
    private String keyword;
}
