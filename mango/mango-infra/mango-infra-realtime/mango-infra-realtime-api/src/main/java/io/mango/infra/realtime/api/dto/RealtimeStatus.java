package io.mango.infra.realtime.api.dto;

import io.mango.common.contract.LocalCapabilityContract;
import io.swagger.v3.oas.annotations.media.Schema;

@LocalCapabilityContract
@Schema(description = "实时消息处理状态")
public record RealtimeStatus(
        @Schema(description = "状态码")
        int code,
        @Schema(description = "状态：SUCCESS/ERROR/PENDING")
        String state) {

    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;

    public static RealtimeStatus success() {
        return new RealtimeStatus(SUCCESS_CODE, "SUCCESS");
    }

    public static RealtimeStatus error() {
        return new RealtimeStatus(ERROR_CODE, "ERROR");
    }
}
