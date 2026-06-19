package io.mango.authorization.api;

import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.common.result.R;

import java.util.List;

/**
 * 主体角色绑定协作 API。
 */
public interface RoleBindingApi {

    /**
     * 按角色业务条件查询角色 ID。
     *
     * @param query 查询条件
     * @return 角色 ID
     */
    R<Long> findRoleId(RoleLookupQuery query);

    /**
     * 确保主体角色绑定存在。
     *
     * @param command 绑定命令
     * @return 是否成功
     */
    R<Boolean> ensureSubjectRoleBinding(SubjectRoleBindingCommand command);

    /**
     * 删除主体角色绑定。
     *
     * @param command 删除命令
     * @return 删除数量
     */
    R<Integer> deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command);

    /**
     * 按角色查询主体 ID。
     *
     * @param query 查询条件
     * @return 主体 ID 列表
     */
    R<List<Long>> listSubjectIdsByRole(SubjectRoleBindingQuery query);
}
