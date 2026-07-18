package io.mango.infra.realtime.api.dto;

import io.mango.common.contract.LocalCapabilityContract;
import io.swagger.v3.oas.annotations.media.Schema;

@LocalCapabilityContract
@Schema(description = "实时流式消息信息")
public record RealtimeStream(
        String id,
        Integer chunk,
        Boolean completed) {
}
