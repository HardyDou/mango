package io.mango.infra.security.core.impl;

import io.mango.infra.security.api.IPermissionProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存存储的默认权限服务实现。
 * <p>
 * 仅在容器中不存在其他 IPermissionProvider Bean 时使用，
 * 例如未引入 mango-authorization-starter 的轻量场景。
 * 生产环境应由授权模块提供正式实现。
 *
 * @author Mango
 */
@Slf4j
public class DefaultPermissionServiceImpl implements IPermissionProvider {

    /**
     * 内存权限存储：userId -> 权限码列表。
     */
    private final Map<Long, List<String>> permissionStore = new ConcurrentHashMap<>();

    @Override
    public List<String> listUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> perms = permissionStore.get(userId);
        if (perms == null) {
            log.debug("No permissions found for userId={}, returning empty list", userId);
            return Collections.emptyList();
        }
        return perms;
    }

    /**
     * 为指定用户添加权限，主要用于测试或简单环境。
     */
    public void addPermissions(Long userId, List<String> permissions) {
        if (userId != null && permissions != null) {
            permissionStore.put(userId, permissions);
        }
    }

    /**
     * 清空全部权限，主要用于测试。
     */
    public void clear() {
        permissionStore.clear();
    }
}
