package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息来源")
public record RealtimeSource(
        @Schema(description = "平台，如 web/ios/android/server")
        String platform,
        @Schema(description = "客户端ID")
        String clientId,
        @Schema(description = "会话ID")
        String sessionId) {

    public RealtimeSource {
        platform = platform == null || platform.isBlank() ? "server" : platform;
    }

    public static RealtimeSource server() {
        return new RealtimeSource("server", null, null);
    }
}
