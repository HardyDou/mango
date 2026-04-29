package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.vo.IdentityUserInfo;

/**
 * 身份用户资料 HTTP 契约。
 */
public interface IdentityUserApi {

    /**
     * 按用户名查询身份资料。
     */
    R<IdentityUserInfo> getUserInfo(String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    R<IdentityUserInfo> getUserInfoById(Long userId);

}
