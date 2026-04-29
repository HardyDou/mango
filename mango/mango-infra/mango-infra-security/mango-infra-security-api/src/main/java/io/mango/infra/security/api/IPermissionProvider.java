package io.mango.infra.security.api;

import java.util.List;

/**
 * 供 Spring Security 的 {@link Perm} 授权流程使用的权限查询接口。
 *
 * @author Mango
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
