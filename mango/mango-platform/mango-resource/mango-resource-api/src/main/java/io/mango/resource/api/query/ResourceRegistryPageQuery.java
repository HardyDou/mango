package io.mango.resource.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资源注册分页查询")
public class ResourceRegistryPageQuery {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Min(value = 1, message = "当前页必须大于等于1")
    @Schema(description = "当前页，从1开始")
    private long page = 1L;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 500, message = "每页大小不能超过500")
    @Schema(description = "每页大小，最大500")
    private long size = DEFAULT_PAGE_SIZE;

    @Size(max = 64, message = "资源类型不能超过64个字符")
    @Schema(description = "资源类型")
    private String resourceType;

    @Size(max = 64, message = "来源模块不能超过64个字符")
    @Schema(description = "来源模块")
    private String moduleCode;

    @Size(max = 64, message = "目标模块不能超过64个字符")
    @Schema(description = "目标模块")
    private String targetModule;

    @Size(max = 32, message = "同步模式不能超过32个字符")
    @Schema(description = "同步模式")
    private String syncMode;

    @Size(max = 32, message = "状态不能超过32个字符")
    @Schema(description = "状态")
    private String status;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词，支持资源ID、BizKey、名称")
    private String keyword;
}
