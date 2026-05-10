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

    @Schema(description = "机构生命周期状态。字典 institution_status：0-禁用，1-启用，2-冻结，9-归档")
    @NotNull(message = "机构状态不能为空")
    private Integer status;
}
