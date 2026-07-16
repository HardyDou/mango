package io.mango.resource.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "资源日志分页查询")
public class ResourceLogPageQuery {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Min(value = 1, message = "当前页必须大于等于1")
    @Schema(description = "当前页，从1开始")
    private long page = 1L;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 500, message = "每页大小不能超过500")
    @Schema(description = "每页大小，最大500")
    private long size = DEFAULT_PAGE_SIZE;

    @Positive(message = "资源注册记录ID必须大于0")
    @Schema(description = "资源注册记录ID")
    private Long resourceId;
}
