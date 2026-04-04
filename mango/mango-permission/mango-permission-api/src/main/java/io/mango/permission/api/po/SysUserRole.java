package io.mango.permission.api.po;

import lombok.Data;

import java.io.Serializable;

/**
 * User-Role relationship PO (API layer - no DB annotations)
 * Used for internal data transfer, not for MyBatis-Plus operations
 *
 * @author Mango
 */
@Data
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Role ID
     */
    private Long roleId;
}
