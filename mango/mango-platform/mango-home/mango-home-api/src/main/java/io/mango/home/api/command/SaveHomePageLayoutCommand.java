package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存用户首页布局命令")
public class SaveHomePageLayoutCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    @Schema(description = "首页ID")
    private Long id;

    @NotBlank(message = "layoutJson不能为空")
    @Size(max = 200000, message = "layoutJson长度不能超过200000")
    @Schema(description = "布局JSON")
    private String layoutJson;
}
