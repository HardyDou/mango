package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人参数配置视图对象。
 */
@Data
@Schema(description = "个人参数配置")
public class PersonalConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "配置分组")
    private String groupCode;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型")
    private String valueType;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
