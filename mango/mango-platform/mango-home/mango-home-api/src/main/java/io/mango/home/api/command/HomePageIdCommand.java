package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户首页ID命令")
public class HomePageIdCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    @Schema(description = "首页ID")
    private Long id;
}
