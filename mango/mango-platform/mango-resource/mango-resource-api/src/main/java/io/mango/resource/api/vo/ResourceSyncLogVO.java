package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "资源同步记录")
public class ResourceSyncLogVO {

    @Schema(description = "记录主键")
    private Long id;
    @Schema(description = "资源注册记录ID")
    private Long resourceId;
    @Schema(description = "同步类型")
    private String syncType;
    @Schema(description = "同步结果")
    private String result;
    @Schema(description = "结果说明")
    private String message;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
