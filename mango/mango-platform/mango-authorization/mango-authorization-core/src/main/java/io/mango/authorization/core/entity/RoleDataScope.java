package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色数据权限配置实体。
 */
@Data
@TableName("authorization_role_data_scope")
public class RoleDataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID。 */
    private Long tenantId;

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
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
