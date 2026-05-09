package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "机构状态修改命令")
public class UpdateTenantStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "机构ID。底层对应 tenantId")
    @NotNull(message = "机构ID不能为空")
    private Long id;

    @Schema(description = "机构状态：0-禁用，1-启用")
    @NotNull(message = "机构状态不能为空")
    private Integer status;
}
