package io.mango.system.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateTenantStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "租户ID不能为空")
    private Long id;

    @NotNull(message = "租户状态不能为空")
    private Integer status;
}
