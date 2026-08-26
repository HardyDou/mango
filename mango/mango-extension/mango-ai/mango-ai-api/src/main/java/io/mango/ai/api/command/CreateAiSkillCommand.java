package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Schema(description = "配置编码")
    private String code;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "显示名称")
    private String name;
    @Size(max = 500)
    @Schema(description = "业务说明")
    private String description;
    @NotBlank
    @Size(max = 65535)
    @Schema(description = "Skill 指令正文")
    private String instructions;
    @Schema(description = "关联工具标识集合")
    @Size(max = 64, message = "一个 Skill 最多关联64个工具")
    private Set<@Positive(message = "工具标识必须大于0") Long> toolIds;
    @NotNull
    @Schema(description = "是否启用")
    private Boolean enabled;

    public Set<Long> getToolIds() {
        return toolIds == null ? null : Set.copyOf(toolIds);
    }

    public void setToolIds(Set<Long> toolIds) {
        this.toolIds = toolIds == null ? null : Set.copyOf(toolIds);
    }
}
