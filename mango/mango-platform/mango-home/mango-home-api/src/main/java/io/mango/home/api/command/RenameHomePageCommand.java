package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "重命名用户首页命令")
public class RenameHomePageCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    @Schema(description = "首页ID")
    private Long id;

    @NotBlank(message = "首页名称不能为空")
    @Size(max = 64, message = "首页名称长度不能超过64")
    @Schema(description = "首页名称")
    private String name;
}
