package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_tenant")
public class SysTenantEntity extends TenantEntity {
    private String tenantName;
    private String tenantCode;
    private String institutionType;
    private String capabilityCodes;
    private Long packageId;
    private Integer status;
    private String contact;
    private String mobile;
    private String email;
    private String remark;
}
