package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成凭证读取记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_credential_access_log")
public class IntegrationCredentialAccessLogEntity extends TenantEntity {

    private String credentialCode;
    private String action;
    private String fieldCodes;
    private Long operatorId;
    private String source;
}
