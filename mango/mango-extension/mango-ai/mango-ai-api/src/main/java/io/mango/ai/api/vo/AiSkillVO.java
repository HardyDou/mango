package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/** Skill 返回对象。 */
@Getter
@Setter
public class AiSkillVO {
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "配置编码")
    private String code;
    @Schema(description = "显示名称")
    private String name;
    @Schema(description = "业务说明")
    private String description;
    @Schema(description = "Skill 指令正文")
    private String instructions;
    @Schema(description = "关联工具标识集合")
    private Set<Long> toolIds;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    public Set<Long> getToolIds() {
        return toolIds == null ? null : Set.copyOf(toolIds);
    }

    public void setToolIds(Set<Long> toolIds) {
        this.toolIds = toolIds == null ? null : Set.copyOf(toolIds);
    }
}
