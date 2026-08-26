package io.mango.ai.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 租户级 Prompt 模板实体。 */
@Getter
@Setter
@TableName("ai_prompt_template")
public class AiPromptEntity extends TenantEntity {
    private String code;
    private String name;
    private String description;
    private String template;
    private String variablesJson;
    private AiPromptStatus status;
    @TableField("template_version")
    private Integer version;
    private LocalDateTime publishedAt;
}
