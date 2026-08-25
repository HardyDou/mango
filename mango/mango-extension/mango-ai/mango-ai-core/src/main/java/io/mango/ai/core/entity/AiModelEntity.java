package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 供应商模型目录实体。 */
@Getter @Setter
@TableName("ai_model")
public class AiModelEntity extends TenantEntity {
    private Long providerConnectionId;
    private String modelName;
    private String displayName;
    private String platformAlias;
    private AiApiProtocol apiProtocol;
    private String capabilitiesJson;
    private String inputModalitiesJson;
    private String outputModalitiesJson;
    private String parameterJson;
    private Boolean enabled;
}
