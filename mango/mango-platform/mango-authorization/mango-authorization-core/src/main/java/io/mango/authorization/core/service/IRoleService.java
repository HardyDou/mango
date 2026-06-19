package io.mango.authorization.core.service;

import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.api.vo.RoleVO;

import java.util.List;

/**
 * 角色内部服务接口。
 */
public interface IRoleService {

    /**
     * 查询全部启用角色，按排序号排序。
     *
     * @return 启用角色列表
     */
    List<RoleVO> list();

    /**
     * 根据 ID 查询角色。
     *
     * @param id 角色 ID
     * @return 角色视图对象，未找到时返回 null
     */
    RoleVO get(Long id);

    /**
     * 创建角色。
     * 租户 ID 自动从 MangoContext 读取。
     *
     * @param po 角色创建数据
     * @return 新角色 ID
     */
    Long create(RoleCommand po);

    /**
     * 更新已有角色。
     *
     * @param po 角色更新数据，必须包含 roleId
     * @return 是否更新成功
     */
    Boolean update(RoleCommand po);

    /**
     * 删除角色以及对应的主体角色、角色菜单关系。
     *
     * @param id 角色 ID
     * @return 是否删除成功
     */
    Boolean delete(Long id);

    /**
     * 查询主体已分配角色。
     *
     * @param subjectId 主体 ID
     * @return 主体角色列表
     */
    List<RoleVO> getSubjectRoles(Long subjectId);

    /**
     * 给主体分配角色。
     */
    Boolean assignRoles(AssignSubjectRolesCommand command);

    /**
     * 按业务条件查询角色 ID。
     *
     * @param query 查询条件
     * @return 角色 ID
     */
    Long findRoleId(RoleLookupQuery query);

    /**
     * 确保主体角色绑定存在。
     *
     * @param command 绑定命令
     * @return 是否成功
     */
    Boolean ensureSubjectRoleBinding(SubjectRoleBindingCommand command);

    /**
     * 删除主体角色绑定。
     *
     * @param command 删除命令
     * @return 删除数量
     */
    Integer deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command);

    /**
     * 按角色查询主体 ID。
     *
     * @param query 查询条件
     * @return 主体 ID 列表
     */
    List<Long> listSubjectIdsByRole(SubjectRoleBindingQuery query);

    /**
     * 查询角色已分配菜单 ID。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 查询当前用户可分配给角色的菜单权限树。
     *
     * @param appCode 应用编码
     * @return 可授权菜单树
     */
    List<MenuVO> listAssignableMenus(String appCode);

    /**
     * 给角色分配菜单。
     * 该操作会替换角色已有菜单关系。
     * 租户 ID 自动从 MangoContext 读取。
     *
     * @param roleId 角色 ID
     * @param menuIds 菜单 ID 列表
     * @return 是否成功
     */
    Boolean assignMenus(Long roleId, List<Long> menuIds);
}
