package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 主体角色关系实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_subject_role")
public class SubjectRoleBindingEntity extends AuthorizationBaseEntity {
    /** 主键 ID。 */

    /** 租户 ID。 */

    /** 主体 ID。 */
    @TableField("subject_id")
    private Long subjectId;

    /** 主体类型。 */
    private String subjectType;

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 归属主体类型。 */
    private String partyType;

    /** 归属主体 ID。 */
    private Long partyId;

    /** 角色 ID。 */
    private Long roleId;
}
