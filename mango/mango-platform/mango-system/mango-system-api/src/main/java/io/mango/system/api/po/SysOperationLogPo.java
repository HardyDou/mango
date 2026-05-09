package io.mango.system.api.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
@Schema(description = "操作日志")
public class SysOperationLogPo {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "操作日志ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作名称")
    private String operation;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "处理器方法")
    private String handlerMethod;

    @Schema(description = "请求路径")
    private String url;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "请求结果")
    private String result;

    @Schema(description = "操作状态")
    private Integer status;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "执行时间(ms)")
    private Long duration;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "IP归属地")
    private String location;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
