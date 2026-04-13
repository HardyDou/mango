package io.mango.rbac.api.po;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System user PO (API layer - no DB annotations)
 * Used for internal data transfer, not for MyBatis-Plus operations
 *
 * @author Mango
 */
@Data
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
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
