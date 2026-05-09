package io.mango.system.api.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
@Schema(description = "登录日志")
public class SysLoginLogPo {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "登录日志ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录类型")
    private String loginType;

    @Schema(description = "登录IP")
    private String ip;

    @Schema(description = "登录地点")
    private String location;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录状态")
    private Integer status;

    @Schema(description = "提示消息")
    private String msg;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
}
