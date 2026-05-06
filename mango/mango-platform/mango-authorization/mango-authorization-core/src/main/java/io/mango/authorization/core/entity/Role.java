package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体。
 */
@Data
@TableName("authorization_role")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long roleId;

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

    /** 角色名称。 */
    private String roleName;

    /** 角色类型：1-系统角色，2-业务角色。 */
    private Integer roleType;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 排序号。 */
    private Integer sort;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 备注。 */
    private String remark;
}
