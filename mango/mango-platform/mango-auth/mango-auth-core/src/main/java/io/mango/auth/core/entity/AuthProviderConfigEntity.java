package io.mango.auth.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_provider_config")
public class AuthProviderConfigEntity extends TenantEntity {

    private String appCode;
    private String provider;
    private String clientId;
    private String providerTenantId;
    private String agentId;
    private String secretCiphertext;
    private String redirectUrisJson;
    private Boolean enabled;
}
