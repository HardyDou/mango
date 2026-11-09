package io.mango.integration.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 连接器定义分页查询。
 */
@Data
@Schema(description = "连接器定义分页查询")
public class IntegrationConnectorPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;

    @Min(value = 1, message = "当前页必须大于等于1")
    @Schema(description = "当前页")
    private long page = 1L;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 200, message = "每页大小不能超过200")
    @Schema(description = "每页条数")
    private long size = DEFAULT_PAGE_SIZE;

    @Size(max = 32, message = "用途分类最多32个字符")
    @Schema(description = "用途分类筛选")
    private String category;

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "连接器编码或名称关键词")
    private String keyword;
}
