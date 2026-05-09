package io.mango.identity.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 身份用户实体。
 */
@Data
@TableName("identity_user")
public class IdentityUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long userId;

    /** 用户名，唯一。 */
    private String username;

    /** 密码哈希。 */
    private String password;

    /** 昵称。 */
    private String nickname;

    /** 登录域。 */
    private String realm;

    /** 操作者类型。 */
    private String actorType;

    /** 归属主体类型。 */
    private String partyType;

    /** 归属主体 ID。 */
    private Long partyId;

    /** 邮箱。 */
    private String email;

    /** 手机号。 */
    private String phone;

    /** 头像地址。 */
    private String avatar;

    /** 状态：0-禁用，1-启用。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginTime;

    /** 备注。 */
    private String remark;

    /** 租户标识。 */
    private String tenantId;
}
