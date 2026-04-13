package io.mango.auth.api.po;

import lombok.Data;

@Data
public class AuthUserInfo {

    private  Long userId;

    private  String username;

    private String nickname;

    private String password;

    private  int status;

}
