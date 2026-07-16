package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "资源变更记录")
public class ResourceChangeLogVO {

    @Schema(description = "记录主键")
    private Long id;
    @Schema(description = "资源注册记录ID")
    private Long resourceId;
    @Schema(description = "变更类型")
    private String changeType;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "变更前内容")
    private String beforeContent;
    @Schema(description = "变更后内容")
    private String afterContent;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
