package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperationLogVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private String tenantId;
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "业务模块")
    private String module;
    @Schema(description = "操作名称")
    private String operation;
    @Schema(description = "请求方法")
    private String method;
    @Schema(description = "处理器方法")
    private String handlerMethod;
    @Schema(description = "请求地址")
    private String url;
    @Schema(description = "请求参数")
    private String params;
    @Schema(description = "请求结果")
    private String result;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "错误信息")
    private String errorMsg;
    @Schema(description = "执行耗时")
    private Long duration;
    @Schema(description = "IP 地址")
    private String ip;
    @Schema(description = "位置")
    private String location;
    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
