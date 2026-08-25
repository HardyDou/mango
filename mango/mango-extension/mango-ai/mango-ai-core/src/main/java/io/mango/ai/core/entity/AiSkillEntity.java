package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/** 租户级 Skill 实体。 */
@Getter
@Setter
@TableName("ai_skill")
public class AiSkillEntity extends TenantEntity {
    private String code;
    private String name;
    private String description;
    private String instructions;
    private String toolIdsJson;
    private Boolean enabled;
}
