package io.mango.authorization.starter.controller;

import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.annotation.InternalAccess;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.authorization.core.service.IRoleService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor
@Validated
@Tag(name = "主体角色绑定", description = "模块间主体角色绑定协作接口")
public class RoleBindingController implements RoleBindingApi {

    private final IRoleService roleService;

    @Override
    @GetMapping("/lookup-id")
    @InternalAccess(desc = "按角色业务条件查询角色 ID")
    @Operation(summary = "按业务条件查询角色ID", description = "内部接口。供模块间协作按业务条件解析角色ID")
    public R<Long> findRoleId(@ParameterObject RoleLookupQuery query) {
        return R.ok(roleService.findRoleId(query));
    }

    @Override
    @PostMapping("/subject-bindings/ensure")
    @InternalAccess(desc = "确保主体角色绑定存在")
    @Operation(summary = "确保主体角色绑定存在", description = "内部接口。供模块间协作维护主体角色绑定")
    public R<Boolean> ensureSubjectRoleBinding(@RequestBody SubjectRoleBindingCommand command) {
        return R.ok(roleService.ensureSubjectRoleBinding(command));
    }

    @Override
    @DeleteMapping("/subject-bindings")
    @InternalAccess(desc = "删除主体角色绑定")
    @Operation(summary = "删除主体角色绑定", description = "内部接口。供模块间协作清理主体角色绑定")
    public R<Integer> deleteSubjectRoleBindings(@RequestBody DeleteSubjectRoleBindingsCommand command) {
        return R.ok(roleService.deleteSubjectRoleBindings(command));
    }

    @Override
    @GetMapping("/subject-bindings/subjects")
    @InternalAccess(desc = "按角色查询主体 ID")
    @Operation(summary = "按角色查询主体ID", description = "内部接口。供模块间协作按角色解析主体ID")
    public R<List<Long>> listSubjectIdsByRole(@ParameterObject SubjectRoleBindingQuery query) {
        return R.ok(roleService.listSubjectIdsByRole(query));
    }
}
