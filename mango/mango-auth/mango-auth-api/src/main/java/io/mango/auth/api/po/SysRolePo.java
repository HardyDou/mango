package io.mango.auth.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * System role PO
 */
@Data
public class SysRolePo extends BasePO {
    private Long roleId;

    @NotBlank(message = "roleCode不能为空")
    @Size(max = 100, message = "roleCode长度不能超过100")
    private String roleCode;

    @NotBlank(message = "roleName不能为空")
    @Size(max = 50, message = "roleName长度不能超过50")
    private String roleName;

    private Integer roleType;
    private Integer status;
    private Integer sort;
    private String remark;
}
