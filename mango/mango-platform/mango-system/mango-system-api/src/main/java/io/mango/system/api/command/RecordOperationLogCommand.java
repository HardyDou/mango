package io.mango.system.api.command;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Max;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordOperationLogCommand {
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
    @Schema(description = "业务模块")
    @Size(max = 64, message = "业务模块长度不正确")
    private String module;
    @Schema(description = "操作名称")
    @Size(max = 100, message = "操作名称长度不正确")
    private String operation;
    @Schema(description = "请求方法")
    @Size(max = 200, message = "请求方法长度不正确")
    private String method;
    @Schema(description = "处理器方法")
    @Size(max = 200, message = "处理器方法长度不正确")
    private String handlerMethod;
    @Size(max = 500, message = "请求地址长度不正确")
    @Schema(description = "请求地址")
    private String url;
    @Schema(description = "请求参数")
    @Size(max = 65535, message = "请求参数长度不正确")
    private String params;
    @Schema(description = "请求结果")
    @Size(max = 65535, message = "请求结果长度不正确")
    private String result;
    @Schema(description = "状态")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;
    @Schema(description = "错误信息")
    @Size(max = 500, message = "错误信息长度不正确")
    private String errorMsg;
    @Schema(description = "执行耗时")
    @Max(value = Long.MAX_VALUE, message = "执行耗时不正确")
    private Long duration;
    @Schema(description = "IP 地址")
    @Size(max = 128, message = "IP 地址长度不正确")
    private String ip;
    @Size(max = 255, message = "位置长度不正确")
    @Schema(description = "位置")
    private String location;
    @Schema(description = "操作时间")
    @PastOrPresent(message = "操作时间不能晚于当前时间")
    private LocalDateTime operateTime;
}
