package io.mango.auth.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * System menu PO for CRUD operations
 */
@Data
public class SysMenuPo extends BasePO {
    private Long menuId;

    private Long tenantId;

    private Long groupId;

    private Long parentId;

    @NotBlank(message = "menuName不能为空")
    @Size(max = 50, message = "menuName长度不能超过50")
    private String menuName;

    private String menuCode;

    private String permission;

    private Integer menuType;
    private Integer sort;
    private String path;
    private String component;
    private String icon;
    private Integer status;
    private Integer visible;
    private Integer keepAlive;
    private Integer embedded;
    private String redirect;
}
