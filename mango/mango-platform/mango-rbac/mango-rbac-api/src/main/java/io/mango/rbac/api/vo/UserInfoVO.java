package io.mango.rbac.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * User info with permissions
 *
 * @author Mango
 */
@Data
public class UserInfoVO {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Username
     */
    private String username;

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
     * Permissions list (authority codes)
     */
    private List<String> permissions = new ArrayList<>();

    /**
     * Roles list
     */
    private List<String> roles = new ArrayList<>();
}
