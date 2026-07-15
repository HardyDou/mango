package io.mango.infra.event.api.query;

import io.mango.infra.event.api.validation.EventOptionalValidation;
import io.mango.infra.kv.api.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统事件分页查询。
 */
@Data
@Schema(description = "系统事件分页查询")
public class SystemEventPageQuery {

    private static final long DEFAULT_PAGE_SIZE = 20L;

    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1L;

    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 200, message = "每页大小不能超过 200")
    @Schema(description = "每页大小")
    private long pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "状态")
    @NotNull(groups = EventOptionalValidation.class)
    private OutboxStatus status;

    @Schema(description = "事件类型")
    @Size(max = 255, message = "事件类型不能超过 255 个字符")
    private String eventType;

    @Schema(description = "业务类型")
    @Size(max = 255, message = "业务类型不能超过 255 个字符")
    private String businessType;

    @Schema(description = "业务主键")
    @Size(max = 255, message = "业务主键不能超过 255 个字符")
    private String businessKey;

    @Schema(description = "关键字，匹配消息 ID、事件类型、业务类型、业务主键或聚合 ID")
    @Size(max = 255, message = "关键字不能超过 255 个字符")
    private String keyword;

    @Schema(description = "是否只查询异常事件")
    @NotNull(message = "异常事件筛选标识不能为空")
    private boolean abnormalOnly = true;
}
