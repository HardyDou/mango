package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建用户首页命令")
public class CreateHomePageCommand implements Serializable {

    @NotBlank(message = "首页名称不能为空")
    @Size(max = 64, message = "首页名称长度不能超过64")
    @Schema(description = "首页名称")
    private String name;

    @Size(max = 200000, message = "布局JSON长度不能超过200000")
    @Schema(description = "布局JSON；为空时使用空布局")
    private String layoutJson;

    @NotNull(message = "是否设置默认首页不能为空")
    @Schema(description = "是否设置为默认首页")
    private Boolean setDefault = false;

    /** 显式 null 与历史逻辑一致，按 false 处理。 */
    public void setSetDefault(Boolean setDefault) {
        this.setDefault = Boolean.TRUE.equals(setDefault);
    }
}
