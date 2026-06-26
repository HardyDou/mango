package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内部认证用户事实。
 */
@Data
@Schema(description = "内部认证用户事实")
public class AuthUserInfo {

    /** 用户 ID。 */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 用户名。 */
    @Schema(description = "用户名")
    private String username;

    /** 昵称。 */
    @Schema(description = "昵称")
    private String nickname;

    /** 登录域。 */
    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER、FINANCIAL")
    private String realm;

    /** 操作者类型。 */
    @Schema(description = "操作者类型，例如 INTERNAL_USER、CUSTOMER_USER、FINANCIAL_USER")
    private String actorType;

    /** 归属主体类型。 */
    @Schema(description = "归属主体类型，例如 COMPANY、FINANCIAL_INSTITUTION、PERSON")
    private String partyType;

    /** 归属主体 ID。 */
    @Schema(description = "归属主体 ID")
    private Long partyId;

    /**
     * 密码哈希，只允许内部认证链路使用。
     */
    @Schema(description = "密码哈希，只允许内部认证链路使用")
    private String password;

    @Schema(description = "是否要求下次登录修改密码")
    private Boolean passwordResetRequired;

    @Schema(description = "最近密码更新时间")
    private LocalDateTime passwordUpdatedAt;

    @Schema(description = "连续登录失败次数")
    private Integer failedLoginCount;

    @Schema(description = "最近登录失败时间")
    private LocalDateTime lastFailedLoginAt;

    @Schema(description = "账号锁定截止时间")
    private LocalDateTime lockedUntil;

    @Schema(description = "账号锁定原因")
    private String lockedReason;

    /** 状态：0-禁用，1-启用。 */
    @Schema(description = "状态：0-禁用，1-启用")
    private int status;
}
