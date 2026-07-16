package io.mango.domain.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新业务域状态命令。
 */
@Data
@Schema(description = "更新业务域状态命令")
public class UpdateDomainStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "业务域ID不能为空")
    @Positive(message = "业务域ID必须大于0")
    @Schema(description = "业务域ID")
    private Long id;

    @NotNull(message = "业务域状态不能为空")
    @Min(value = 0, message = "业务域状态非法")
    @Max(value = 1, message = "业务域状态非法")
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
