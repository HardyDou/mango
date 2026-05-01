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

    /**
     * 查询指定认证主体在当前上下文中拥有的权限码。
     *
     * @param principal 认证主体
     * @return 权限码列表
     */
    default List<String> listUserPermissions(SecurityPrincipal principal) {
        return principal == null ? List.of() : listUserPermissions(principal.userId());
    }
}
