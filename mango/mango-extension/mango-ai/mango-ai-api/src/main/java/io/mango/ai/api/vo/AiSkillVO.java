package io.mango.ai.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/** Skill 返回对象。 */
@Getter
@Setter
public class AiSkillVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String instructions;
    private Set<Long> toolIds;
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
