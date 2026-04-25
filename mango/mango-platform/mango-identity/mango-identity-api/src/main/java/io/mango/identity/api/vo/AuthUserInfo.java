package io.mango.identity.api.vo;

import lombok.Data;

/**
 * Internal authentication user facts.
 */
@Data
public class AuthUserInfo {

    private Long userId;

    private String username;

    private String nickname;

    private String password;

    private int status;
}
