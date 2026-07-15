package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * RoleEntity-MenuEntity relationship entity
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_role_menu")
public class RoleMenuEntity extends AuthorizationBaseEntity {
    /**
     * ID
     */

    /**
     * Tenant ID
     */

    /**
     * RoleEntity ID
     */
    private Long roleId;

    /**
     * MenuEntity ID
     */
    private Long menuId;
}
