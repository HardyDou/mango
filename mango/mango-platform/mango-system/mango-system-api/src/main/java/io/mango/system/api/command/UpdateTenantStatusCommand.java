package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "租户状态修改命令")
public class UpdateTenantStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户ID")
    @NotNull(message = "租户ID不能为空")
    private Long id;

    @Schema(description = "租户状态：0-禁用，1-启用")
    @NotNull(message = "租户状态不能为空")
    private Integer status;
}
