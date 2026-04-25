package io.mango.authorization.api.po;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System role PO (API layer - no DB annotations)
 * Used for internal data transfer, not for MyBatis-Plus operations
 *
 * @author Mango
 */
@Data
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * Role code (unique)
     */
    private String roleCode;

    /**
     * Role name
     */
    private String roleName;

    /**
     * Role type (1: system, 2: business)
     */
    private Integer roleType;

    /**
     * Status (0: disabled, 1: enabled)
     */
    private Integer status;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Create time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;

    /**
     * Remark
     */
    private String remark;
}
