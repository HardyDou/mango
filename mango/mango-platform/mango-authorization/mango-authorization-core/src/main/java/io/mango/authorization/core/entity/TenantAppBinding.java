package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端租户应用开通关系。
 * <p>
 * 记录租户可访问的前端应用入口，不改动授权应用基础表。
 */
@Data
@TableName("frontend_tenant_app_binding")
public class TenantAppBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 绑定 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long bindingId;

    /** 租户 ID。 */
    private Long tenantId;

    /** 应用编码。 */
    private String appCode;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 过期时间。 */
    private LocalDateTime expireTime;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
