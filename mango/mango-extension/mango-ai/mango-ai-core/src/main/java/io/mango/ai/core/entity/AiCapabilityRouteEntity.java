package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiCapability;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** 租户 AI 能力默认路由实体。 */
@Getter @Setter
@TableName("ai_capability_route")
public class AiCapabilityRouteEntity extends TenantEntity {
    private AiCapability capability;
    private Long modelId;
}
