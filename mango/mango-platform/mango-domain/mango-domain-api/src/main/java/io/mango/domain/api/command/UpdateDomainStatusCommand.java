package io.mango.domain.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新业务域状态命令。
 */
@Data
@Schema(description = "更新业务域状态命令")
public class UpdateDomainStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "业务域ID")
    private Long id;

    @NotNull
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
