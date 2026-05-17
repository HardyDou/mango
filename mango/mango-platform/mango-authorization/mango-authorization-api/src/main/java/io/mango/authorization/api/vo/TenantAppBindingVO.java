package io.mango.authorization.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户应用开通关系 VO。
 */
@Data
public class TenantAppBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bindingId;
    private Long tenantId;
    private String appCode;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
