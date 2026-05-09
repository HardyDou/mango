package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String handlerMethod;
    private String url;
    private String params;
    private String result;
    private Integer status;
    private String errorMsg;
    private Long duration;
    private String ip;
    private String location;
    private LocalDateTime operateTime;
}
