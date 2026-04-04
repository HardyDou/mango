package io.mango.auth.core.service;

import io.mango.auth.api.po.SysRolePo;
import io.mango.auth.api.vo.SysRoleVO;

import java.util.List;

public interface ISysRoleService {
    List<SysRoleVO> list();
    SysRoleVO get(Long id);
    Long create(SysRolePo po);
    Boolean update(SysRolePo po);
    Boolean delete(Long id);
    List<SysRoleVO> getUserRoles(Long userId);
    Boolean assignRoles(Long userId, List<Long> roleIds);
    List<Long> getRoleMenuIds(Long roleId);
    Boolean assignMenus(Long roleId, List<Long> menuIds);
}
