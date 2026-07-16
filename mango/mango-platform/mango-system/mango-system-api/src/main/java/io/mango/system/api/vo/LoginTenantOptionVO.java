package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginTenantOptionVO {
    @Schema(description = "租户 ID")
    private String tenantId;
    @Schema(description = "租户编码")
    private String tenantCode;
    @Schema(description = "租户名称")
    private String tenantName;
}
