package io.mango.auth.starter.controller;

import io.mango.auth.api.SysRoleApi;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class SysUserAuthController {

    private final SysRoleApi sysRoleApi;

    @GetMapping("/{id}/roles")
    @Perm("auth:user:query")
    public R<List<SysRoleVO>> getUserRoles(@PathVariable Long id) {
        return sysRoleApi.getUserRoles(id);
    }

    @PutMapping("/{id}/roles")
    @Perm("auth:user:authorize")
    public R<Boolean> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        return sysRoleApi.assignRoles(id, roleIds);
    }
}
