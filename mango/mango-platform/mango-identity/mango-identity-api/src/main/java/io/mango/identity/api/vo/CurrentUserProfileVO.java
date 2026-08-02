package io.mango.identity.api.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CurrentUserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String realName;
    private String documentType;
    private String documentNumber;
    private String verificationStatus;
    private String verificationSource;
}
