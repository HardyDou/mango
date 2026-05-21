package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息确认信息")
public record RealtimeAck(
        @Schema(description = "是否要求确认")
        Boolean required,
        @Schema(description = "被确认的消息ID")
        String messageId,
        @Schema(description = "是否已接收")
        Boolean accepted) {

    public static RealtimeAck requireAck() {
        return new RealtimeAck(true, null, null);
    }

    public static RealtimeAck accepted(String messageId) {
        return new RealtimeAck(null, messageId, true);
    }
}
