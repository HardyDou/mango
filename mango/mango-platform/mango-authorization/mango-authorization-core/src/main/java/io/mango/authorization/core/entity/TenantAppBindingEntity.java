package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * 前端租户应用开通关系。
 * <p>
 * 记录租户可访问的前端应用入口，不改动授权应用基础表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("frontend_tenant_app_binding")
public class TenantAppBindingEntity extends AuthorizationBaseEntity {
    /** 绑定 ID 兼容访问器，底层统一使用 Mango 主键。 */
    public Long getBindingId() {
        return getId();
    }

    public void setBindingId(Long bindingId) {
        setId(bindingId);
    }

    /** 租户 ID。 */

    /** 应用编码。 */
    private String appCode;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 过期时间。 */
    private LocalDateTime expireTime;

    /** 创建时间。 */

    /** 更新时间。 */
}
