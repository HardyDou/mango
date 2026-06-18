package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.api.vo.RoleDataScopeVO;

import java.util.List;

/**
 * 角色数据权限服务。
 */
public interface IRoleDataScopeService {

    List<RoleDataScopeVO> listByRole(Long roleId);

    Boolean save(SaveRoleDataScopeCommand command);

    Boolean delete(Long roleId, String resourceCode);

    EffectiveDataScopeVO resolve(AuthorizationQuery query, String resourceCode);
}
