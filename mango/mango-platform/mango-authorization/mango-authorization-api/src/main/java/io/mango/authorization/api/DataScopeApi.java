package io.mango.authorization.api;

import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.query.EffectiveDataScopeQuery;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.api.vo.RoleDataScopeVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 数据权限 API 契约。
 */
public interface DataScopeApi {

    /**
     * 查询角色的数据权限配置。
     *
     * @param roleId 角色 ID。
     * @return 角色数据权限配置。
     */
    R<List<RoleDataScopeVO>> listRoleScopes(@Positive Long roleId);

    /**
     * 保存角色数据权限配置。
     *
     * @param command 保存命令。
     * @return 是否成功。
     */
    R<Boolean> saveRoleScope(@Valid SaveRoleDataScopeCommand command);

    /**
     * 删除角色数据权限配置。
     *
     * @param roleId 角色 ID。
     * @param resourceCode 资源编码。
     * @return 是否成功。
     */
    R<Boolean> deleteRoleScope(@Positive Long roleId, @NotBlank String resourceCode);

    /**
     * 查询当前主体在指定资源上的生效数据权限。
     *
     * @param query 查询参数。
     * @return 生效数据权限。
     */
    R<EffectiveDataScopeVO> effective(@Valid EffectiveDataScopeQuery query);
}
