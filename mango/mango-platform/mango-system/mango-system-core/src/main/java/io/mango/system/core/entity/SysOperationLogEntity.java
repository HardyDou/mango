package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_operation_log")
public class SysOperationLogEntity extends TenantEntity {
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
