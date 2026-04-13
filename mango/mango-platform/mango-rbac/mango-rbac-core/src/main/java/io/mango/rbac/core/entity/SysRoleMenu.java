package io.mango.rbac.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * Role-Menu relationship entity
 *
 * @author Mango
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Tenant ID
     */
    private Long tenantId;

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * Menu ID
     */
    private Long menuId;
}
