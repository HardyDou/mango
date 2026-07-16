package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysLoginLogVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private String tenantId;
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "登录类型")
    private String loginType;
    @Schema(description = "IP 地址")
    private String ip;
    @Schema(description = "位置")
    private String location;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "提示消息")
    private String msg;
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
}
