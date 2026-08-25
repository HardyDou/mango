package io.mango.ai.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** 创建 Skill。 */
@Getter
@Setter
public class CreateAiSkillCommand {
    @NotBlank
    @Size(max = 64)
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    @NotBlank
    @Size(max = 65535)
    private String instructions;
    private Set<Long> toolIds;
    @NotNull
    private Boolean enabled;

    public Set<Long> getToolIds() {
        return toolIds == null ? null : Set.copyOf(toolIds);
    }

    public void setToolIds(Set<Long> toolIds) {
        this.toolIds = toolIds == null ? null : Set.copyOf(toolIds);
    }
}
