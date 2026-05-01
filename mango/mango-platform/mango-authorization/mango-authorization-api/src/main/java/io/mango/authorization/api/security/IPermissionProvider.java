package io.mango.authorization.api.security;

import java.util.List;

/**
 * 权限码查询技术接口，供请求级访问策略使用。
 */
public interface IPermissionProvider {

    /**
     * 查询指定用户拥有的全部权限码。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    List<String> listUserPermissions(Long userId);
}
