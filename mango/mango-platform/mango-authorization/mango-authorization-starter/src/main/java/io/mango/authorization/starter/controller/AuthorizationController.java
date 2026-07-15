package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.core.service.IAuthorizationQueryService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向远程适配器的授权查询端点。
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
@Validated
@Tag(name = "授权查询", description = "主体角色权限授权查询接口")
public class AuthorizationController implements AuthorizationApi {

    private final IAuthorizationQueryService authorizationQueryService;

    @Override
    @GetMapping("/subjects/user")
    @Operation(summary = "查询成员授权快照", description = "内部接口。根据机构成员主体、登录域、操作者类型和归属主体查询角色与权限快照")
    public R<AuthorizationSnapshotVO> loadUserAuthorization(@ParameterObject LoadUserAuthorizationQuery query) {
        return R.ok(authorizationQueryService.loadUserAuthorization(query));
    }
}
