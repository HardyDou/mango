package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("identity_external_binding")
public class ExternalIdentityBindingEntity extends TenantEntity {

    private String appCode;

    private Long userId;

    private String provider;

    private String corpId;

    private String externalUserId;

    private String displayName;

    private String bindSource;

    private String bindStatus;

    private LocalDateTime bindTime;

    private LocalDateTime lastLoginTime;

}
