package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户应用开通命令。
 */
@Data
@Schema(description = "租户应用开通命令")
public class TenantAppBindingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户应用绑定ID，修改时必填")
    private Long bindingId;

    @Schema(description = "租户ID")
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @Schema(description = "应用编码")
    @NotBlank(message = "应用编码不能为空")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @Schema(description = "状态：0-停用，1-启用")
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    private Integer status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
