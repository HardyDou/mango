package io.mango.identity.core.adapter;

import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 隔离授权域远程协议，为身份域提供角色绑定协作能力。 */
@Component
@RequiredArgsConstructor
public class AuthorizationRoleBindingAdapter {

    private final RoleBindingApi roleBindingApi;

    public void deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command) {
        roleBindingApi.deleteSubjectRoleBindings(command);
    }

    public Long findRoleId(RoleLookupQuery query) {
        R<Long> result = roleBindingApi.findRoleId(query);
        return result != null && result.isSuccess() ? result.getData() : null;
    }

    public void ensureSubjectRoleBinding(SubjectRoleBindingCommand command) {
        roleBindingApi.ensureSubjectRoleBinding(command);
    }

    public List<Long> listSubjectIdsByRole(SubjectRoleBindingQuery query) {
        R<List<Long>> result = roleBindingApi.listSubjectIdsByRole(query);
        return result != null && result.isSuccess() && result.getData() != null ? result.getData() : List.of();
    }
}
