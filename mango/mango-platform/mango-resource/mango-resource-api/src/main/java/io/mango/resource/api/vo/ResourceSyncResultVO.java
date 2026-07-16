package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
}
