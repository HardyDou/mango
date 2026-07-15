package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 租户应用开通关系查询条件。 */
@Data
@Schema(description = "租户应用开通关系查询条件")
public class TenantAppBindingQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive
    @Schema(description = "租户ID")
    private Long tenantId;

    @Size(max = 64)
    @Schema(description = "应用编码")
    private String appCode;

    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
