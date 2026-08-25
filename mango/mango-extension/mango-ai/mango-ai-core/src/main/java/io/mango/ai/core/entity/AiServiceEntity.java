package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** 租户级 AI 服务定义实体。 */
@Getter
@Setter
@TableName("ai_service_definition")
public class AiServiceEntity extends TenantEntity {
    private String code;
    private String name;
    private String description;
    private AiServiceType serviceType;
    private AiCapability capability;
    private Long promptId;
    private Long skillId;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Boolean enabled;
}
