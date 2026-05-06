package io.mango.identity.api.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 认证用户名查询条件。
 */
@Data
public class AuthUsernameQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private String realm;

    private String username;
}
