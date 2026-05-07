package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实时消息轮询查询条件。
 */
@Data
@Schema(description = "实时消息轮询查询条件")
public class RealtimePollingQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "本次最多返回消息数量")
    private Integer maxSize;

    @Schema(description = "长轮询等待超时时间，单位毫秒")
    private Long timeoutMillis;
}
