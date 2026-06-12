package io.mango.infra.event.api.query;

import io.mango.infra.kv.api.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 系统事件分页查询。
 */
@Data
@Schema(description = "系统事件分页查询")
public class SystemEventPageQuery {

    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1L;

    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 200, message = "每页大小不能超过 200")
    @Schema(description = "每页大小")
    private long pageSize = 20L;

    @Schema(description = "状态")
    private OutboxStatus status;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "关键字，匹配消息 ID、事件类型、业务类型、业务主键或聚合 ID")
    private String keyword;

    @Schema(description = "是否只查询异常事件")
    private boolean abnormalOnly = true;
}
