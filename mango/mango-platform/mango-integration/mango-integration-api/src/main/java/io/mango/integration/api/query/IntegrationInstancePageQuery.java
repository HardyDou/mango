package io.mango.integration.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 连接实例分页查询。
 */
@Data
@Schema(description = "连接实例分页查询")
public class IntegrationInstancePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "当前页必须大于等于1")
    @Schema(description = "当前页")
    private long page = 1L;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 200, message = "每页大小不能超过200")
    @Schema(description = "每页条数")
    private long size = 20L;

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "实例编码或名称关键词")
    private String keyword;

    @Size(max = 64, message = "连接器编码最多64个字符")
    @Schema(description = "连接器编码筛选")
    private String connectorCode;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态筛选：0-停用，1-启用")
    private Integer status;
}
