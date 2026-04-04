package io.mango.permission.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System user entity
 *
 * @author Mango
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    /**
     * Username (unique)
     */
    private String username;

    /**
     * Password (encrypted)
     */
    private String password;

    /**
     * Nickname
     */
    private String nickname;

    /**
     * Email
     */
    private String email;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar URL
     */
    private String avatar;

    /**
     * Status (0: disabled, 1: enabled)
     */
    private Integer status;

    /**
     * Create time
     */
    private LocalDateTime createTime;

    /**
     * Update time
     */
    private LocalDateTime updateTime;

    /**
     * Last login time
     */
    private LocalDateTime lastLoginTime;

    /**
     * Remark
     */
    private String remark;
}
