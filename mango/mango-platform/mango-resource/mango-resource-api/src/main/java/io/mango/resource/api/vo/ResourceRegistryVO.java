package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "资源注册记录")
public class ResourceRegistryVO {

    @Schema(description = "记录主键")
    private Long id;
    @Schema(description = "稳定资源ID")
    private String resourceId;
    @Schema(description = "资源版本")
    private Integer resourceVersion;
    @Schema(description = "资源类型")
    private String resourceType;
    @Schema(description = "来源模块")
    private String moduleCode;
    @Schema(description = "业务稳定键")
    private String bizKey;
    @Schema(description = "资源名称")
    private String name;
    @Schema(description = "目标模块")
    private String targetModule;
    @Schema(description = "目标表")
    private String targetTable;
    @Schema(description = "目标数据主键")
    private Long targetId;
    @Schema(description = "声明内容摘要")
    private String sourceHash;
    @Schema(description = "同步模式")
    private String syncMode;
    @Schema(description = "资源状态")
    private String status;
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
