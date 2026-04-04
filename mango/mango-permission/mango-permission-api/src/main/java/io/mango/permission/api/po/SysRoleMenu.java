package io.mango.permission.api.po;

import lombok.Data;

import java.io.Serializable;

/**
 * Role-Menu relationship PO (API layer - no DB annotations)
 * Used for internal data transfer, not for MyBatis-Plus operations
 *
 * @author Mango
 */
@Data
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * Menu ID
     */
    private Long menuId;
}
