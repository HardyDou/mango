package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** Login account availability for the current tenant. */
@Data
@Schema(description = "登录账号可用性")
public class IdentityAccountAvailabilityVO {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String RECOVERABLE = "RECOVERABLE";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    @Schema(description = "AVAILABLE、RECOVERABLE 或 UNAVAILABLE")
    private String status;

    @Schema(description = "原成员姓名，仅可恢复时返回")
    private String displayName;

    @Schema(description = "脱敏手机号，仅可恢复时返回")
    private String maskedPhone;

    @Schema(description = "脱敏邮箱，仅可恢复时返回")
    private String maskedEmail;

    @Schema(description = "成员编号，仅可恢复时返回")
    private String memberNo;

    @Schema(description = "移出时间，仅可恢复时返回")
    private LocalDateTime removedAt;
}
