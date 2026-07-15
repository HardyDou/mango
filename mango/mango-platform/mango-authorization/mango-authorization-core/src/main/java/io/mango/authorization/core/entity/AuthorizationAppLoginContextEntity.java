package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 授权应用登录上下文实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_app_login_context")
public class AuthorizationAppLoginContextEntity extends AuthorizationBaseEntity {
    /** 上下文 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getContextId() {
        return getId();
    }

    public void setContextId(Long contextId) {
        setId(contextId);
    }

    /** 应用 ID。 */
    private Long appId;

    /** 应用编码。 */
    private String appCode;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 是否默认上下文：0-否，1-是。 */
    private Integer defaultFlag;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */

    /** 更新时间。 */
}
