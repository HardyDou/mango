package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiProviderType;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 租户级 AI 厂商接入实体。 */
@Getter @Setter
@TableName("ai_provider_connection")
public class AiProviderConnectionEntity extends TenantEntity {
    private String code;
    private String displayName;
    private AiProviderType providerType;
    private String baseUrl;
    private String apiKeyCiphertext;
    private String apiKeyHint;
    private Boolean enabled;
}
