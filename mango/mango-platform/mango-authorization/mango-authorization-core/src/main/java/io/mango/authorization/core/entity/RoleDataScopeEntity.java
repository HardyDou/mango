package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 角色数据权限配置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_role_data_scope")
public class RoleDataScopeEntity extends AuthorizationBaseEntity {
    /** 主键 ID。 */

    /** 租户 ID。 */

    /** 应用编码。 */
    private String appCode;

    /** 角色 ID。 */
    private Long roleId;

    /** 资源编码。 */
    private String resourceCode;

    /** 范围模式：ALL/SELF/SELF_ORG/SELF_ORG_AND_CHILDREN/ORG。 */
    private String scopeMode;

    /** 范围值 JSON 数组。 */
    private String scopeValues;

    /** 是否包含下级组织。 */
    private Boolean includeChildren;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 创建时间。 */

    /** 更新时间。 */
}
