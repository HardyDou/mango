package io.mango.system.api.command;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Max;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordLoginLogCommand {
    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID必须大于 0")
    private Long id;
    @Schema(description = "租户 ID")
    @Size(max = 64, message = "租户 ID长度不正确")
    private String tenantId;
    @Schema(description = "用户 ID")
    @Positive(message = "用户 ID必须大于 0")
    private Long userId;
    @Schema(description = "用户名")
    @Size(max = 64, message = "用户名长度不正确")
    private String username;
    @Schema(description = "登录类型")
    @Size(max = 20, message = "登录类型长度不正确")
    private String loginType;
    @Schema(description = "IP 地址")
    @Size(max = 128, message = "IP 地址长度不正确")
    private String ip;
    @Schema(description = "位置")
    @Size(max = 255, message = "位置长度不正确")
    private String location;
    @Schema(description = "浏览器")
    @Size(max = 128, message = "浏览器长度不正确")
    private String browser;
    @Size(max = 64, message = "操作系统长度不正确")
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "状态")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;
    @Schema(description = "提示消息")
    @Size(max = 500, message = "提示消息长度不正确")
    private String msg;
    @Schema(description = "登录时间")
    @PastOrPresent(message = "登录时间不能晚于当前时间")
    private LocalDateTime loginTime;
}
