package io.mango.system.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysTenantPo extends BasePO {
    private Long id;

    @NotBlank(message = "tenantName不能为空")
    @Size(max = 100, message = "tenantName长度不能超过100")
    private String tenantName;

    @NotBlank(message = "tenantCode不能为空")
    @Size(max = 50, message = "tenantCode长度不能超过50")
    private String tenantCode;

    @NotNull(message = "status不能为空")
    private Integer status;

    private String contact;
    private String mobile;
    private String email;
    private String remark;
}
