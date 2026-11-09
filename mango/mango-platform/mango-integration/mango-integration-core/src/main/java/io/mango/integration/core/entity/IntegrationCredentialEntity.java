package io.mango.integration.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 集成凭证实体。
 *
 * <p>敏感字段以密文保存，版本按字段记录，响应只返回掩码。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_credential")
public class IntegrationCredentialEntity extends TenantEntity {

    private String credentialCode;
    private String connectorCode;
    private String fieldsJson;
    private String fieldVersionsJson;
    private LocalDateTime rotatedAt;
    private LocalDateTime expiresAt;
    private String status;
    private Integer revision;
    private String remark;
}
