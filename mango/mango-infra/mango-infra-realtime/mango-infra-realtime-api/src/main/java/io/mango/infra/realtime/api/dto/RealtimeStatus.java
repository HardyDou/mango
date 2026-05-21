package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息处理状态")
public record RealtimeStatus(
        @Schema(description = "状态码")
        int code,
        @Schema(description = "状态：SUCCESS/ERROR/PENDING")
        String state) {

    public static RealtimeStatus success() {
        return new RealtimeStatus(200, "SUCCESS");
    }

    public static RealtimeStatus error() {
        return new RealtimeStatus(500, "ERROR");
    }
}
