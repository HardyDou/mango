package io.mango.identity.core.service;

import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.api.vo.IdentityUserInfo;

/**
 * 身份用户服务接口。
 */
public interface IIdentityUserService {

    /**
     * 按用户名查询身份资料。
     */
    IdentityUserInfo getUserInfo(String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    IdentityUserInfo getUserInfoById(Long userId);

    /**
     * 按用户名查询用户实体。
     */
    IdentityUser getByUsername(String username);

    /**
     * 按登录域和用户名查询用户实体。
     */
    IdentityUser getByUsername(String username, String realm);

    /**
     * 按用户 ID 查询用户实体。
     */
    IdentityUser getById(Long userId);

}
