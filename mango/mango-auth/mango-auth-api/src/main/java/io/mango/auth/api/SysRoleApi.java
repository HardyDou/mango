package io.mango.auth.api;

import io.mango.auth.api.po.SysRolePo;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * System role API
 */
public interface SysRoleApi {

    R<List<SysRoleVO>> list();

    R<SysRoleVO> get(Long id);

    R<Long> create(SysRolePo po);

    R<Boolean> update(SysRolePo po);

    R<Boolean> delete(Long id);

    R<List<SysRoleVO>> getUserRoles(Long userId);

    R<Boolean> assignRoles(Long userId, List<Long> roleIds);

    R<List<Long>> getRoleMenuIds(Long roleId);

    R<Boolean> assignMenus(Long roleId, List<Long> menuIds);
}
