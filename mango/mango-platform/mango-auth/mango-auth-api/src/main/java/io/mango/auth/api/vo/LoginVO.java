package io.mango.auth.api.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录结果 VO。
 */
@Data
public class LoginVO {

    private String accessToken;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private String refreshToken;

    private Long userId;

    private String username;

    private String nickname;

    private String realm;

    private String actorType;

    private String partyType;

    private Long partyId;

    private String appCode;

    private List<String> roles;

    private List<String> permissions;
}
