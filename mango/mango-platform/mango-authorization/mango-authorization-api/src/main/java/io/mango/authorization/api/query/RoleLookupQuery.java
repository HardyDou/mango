package io.mango.authorization.api.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色业务条件查询。
 */
@Data
public class RoleLookupQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID。 */
    private Long tenantId;

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 角色编码。 */
    private String roleCode;
}
