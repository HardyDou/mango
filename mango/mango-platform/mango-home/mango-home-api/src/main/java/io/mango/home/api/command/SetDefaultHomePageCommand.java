package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设置默认首页命令")
public class SetDefaultHomePageCommand implements Serializable {

    @NotBlank(message = "首页标识不能为空")
    @Schema(description = "首页路由标识。个人首页为数字ID，授权模板为 template:{templateId}")
    private String homeId;
}
