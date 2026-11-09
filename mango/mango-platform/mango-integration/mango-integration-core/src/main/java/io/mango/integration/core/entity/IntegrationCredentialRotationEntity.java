package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成凭证轮换记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_credential_rotation")
public class IntegrationCredentialRotationEntity extends TenantEntity {

    private String credentialCode;
    private Integer version;
    private String fieldCodes;
    private Long operatorId;
    private String remark;
}
