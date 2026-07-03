package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建首页模板命令")
public class CreateHomeTemplateCommand implements Serializable {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称长度不能超过64")
    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "草稿布局JSON")
    private String layoutJson;
}
