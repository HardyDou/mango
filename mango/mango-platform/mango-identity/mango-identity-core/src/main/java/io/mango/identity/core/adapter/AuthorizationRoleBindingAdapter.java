package io.mango.identity.core.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The API client is an intentionally shared Spring collaborator")
public class AuthorizationRoleBindingAdapter {

    private final RoleBindingApi roleBindingApi;

    public void deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command) {
        roleBindingApi.deleteSubjectRoleBindings(command);
    }

    public Long findRoleId(RoleLookupQuery query) {
        R<Long> result = roleBindingApi.findRoleId(query);
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result.getData();
    }

    public void ensureSubjectRoleBinding(SubjectRoleBindingCommand command) {
        roleBindingApi.ensureSubjectRoleBinding(command);
    }

    public List<Long> listSubjectIdsByRole(SubjectRoleBindingQuery query) {
        R<List<Long>> result = roleBindingApi.listSubjectIdsByRole(query);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return List.of();
        }
        return result.getData();
    }
}
