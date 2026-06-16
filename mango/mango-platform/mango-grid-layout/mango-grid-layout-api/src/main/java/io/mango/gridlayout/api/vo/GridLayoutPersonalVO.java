package io.mango.gridlayout.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户自定义栅格布局。
 */
@Data
@Schema(description = "当前登录用户自定义栅格布局")
public class GridLayoutPersonalVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "页面编码")
    private String pageCode;

    @Schema(description = "布局结构版本")
    private Integer schemaVersion;

    @Schema(description = "布局 JSON")
    private String layoutJson;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
