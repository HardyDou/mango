package io.mango.gridlayout.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户自定义栅格布局查询条件。
 */
@Data
@Schema(description = "当前登录用户自定义栅格布局查询条件")
public class GridLayoutPersonalQuery {

    @Schema(description = "页面编码，例如 admin-home-workbench")
    @NotBlank(message = "pageCode不能为空")
    @Size(max = 100, message = "pageCode长度不能超过100")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "pageCode只能包含字母、数字、点、下划线、冒号和短横线")
    private String pageCode;
}
