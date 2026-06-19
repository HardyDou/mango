package io.mango.authorization.starter.remote;

import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 角色绑定远程客户端。
 */
@FeignClient(name = "mango-authorization", contextId = "roleBindingFeignClient", path = "/authorization")
public interface RoleBindingFeignClient extends RoleBindingApi {

    @Override
    @GetMapping("/roles/lookup-id")
    R<Long> findRoleId(@SpringQueryMap RoleLookupQuery query);

    @Override
    @PostMapping("/roles/subject-bindings/ensure")
    R<Boolean> ensureSubjectRoleBinding(@RequestBody SubjectRoleBindingCommand command);

    @Override
    @DeleteMapping("/roles/subject-bindings")
    R<Integer> deleteSubjectRoleBindings(@RequestBody DeleteSubjectRoleBindingsCommand command);

    @Override
    @GetMapping("/roles/subject-bindings/subjects")
    R<List<Long>> listSubjectIdsByRole(@SpringQueryMap SubjectRoleBindingQuery query);
}
