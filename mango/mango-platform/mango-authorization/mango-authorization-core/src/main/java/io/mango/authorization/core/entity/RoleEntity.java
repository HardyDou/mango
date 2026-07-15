package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 角色实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_role")
public class RoleEntity extends AuthorizationBaseEntity {
    /** 角色 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getRoleId() {
        return getId();
    }

    public void setRoleId(Long roleId) {
        setId(roleId);
    }

    /** 租户 ID。 */

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 角色编码。 */
    private String roleCode;

    /** 角色名称。 */
    private String roleName;

    /** 角色类型：1-系统角色，2-业务角色。 */
    private Integer roleType;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */

    /** 更新时间。 */

    /** 备注。 */
    private String remark;
}
