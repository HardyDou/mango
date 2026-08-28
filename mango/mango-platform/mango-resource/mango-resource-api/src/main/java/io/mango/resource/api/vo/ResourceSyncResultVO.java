package io.mango.resource.api.vo;

import io.mango.resource.api.enums.ResourceSyncDisposition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源同步结果。
 */
@Data
@Schema(description = "资源同步结果")
public class ResourceSyncResultVO {

    @Schema(description = "目标数据主键")
    private Long targetId;

    @Schema(description = "目标数据表")
    private String targetTable;

    @Schema(description = "结果说明")
    private String message;

    @Schema(description = "同步结果类型")
    private ResourceSyncDisposition disposition;

    @Schema(description = "Handler 实际写入目标数据的固定同步时间")
    private LocalDateTime synchronizationTime;
}
