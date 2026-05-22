package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息投递目标类型")
public enum RealtimeTargetType {
    USER,
    CLIENT,
    CONNECTION,
    GROUP,
    TENANT,
    BROADCAST
}
