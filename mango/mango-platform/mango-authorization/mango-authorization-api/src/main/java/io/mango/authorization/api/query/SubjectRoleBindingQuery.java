package io.mango.authorization.api.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 主体角色绑定查询。
 */
@Data
public class SubjectRoleBindingQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID。 */
    private Long tenantId;

    /** 主体类型。 */
    private String subjectType;

    /** 角色 ID。 */
    private Long roleId;

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
}
