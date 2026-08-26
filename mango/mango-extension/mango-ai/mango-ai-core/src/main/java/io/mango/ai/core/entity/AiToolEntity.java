package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiToolType;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** 租户级 AI 工具实体。 */
@Getter
@Setter
@TableName("ai_tool")
public class AiToolEntity extends TenantEntity {
    private String code;
    private String name;
    private String description;
    private AiToolType toolType;
    private String endpoint;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Boolean enabled;
}
