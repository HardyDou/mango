package io.mango.rbac.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System role entity
 *
 * @author Mango
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Role ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long roleId;

    /**
     * Tenant ID
     */
    private Long tenantId;

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
