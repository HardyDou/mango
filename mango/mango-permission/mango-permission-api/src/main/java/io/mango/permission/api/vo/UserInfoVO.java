package io.mango.permission.api.vo;

import io.mango.common.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * User info with permissions
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserInfoVO extends BaseVO {

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
