package io.mango.home.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.home.api.HomeTemplateApi;
import io.mango.home.api.command.CreateHomeTemplateCommand;
import io.mango.home.api.command.HomeTemplateIdCommand;
import io.mango.home.api.command.SaveHomeTemplateAuthorizationsCommand;
import io.mango.home.api.command.UpdateHomeTemplateDraftCommand;
import io.mango.home.api.command.UpdateHomeTemplateStatusCommand;
import io.mango.home.api.query.HomeTemplateAuthorizationQuery;
import io.mango.home.api.query.HomeTemplateQuery;
import io.mango.home.api.query.UserHomeViewQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.api.vo.HomeTemplateAuthorizationVO;
import io.mango.home.api.vo.HomeTemplateVO;
import io.mango.home.core.service.IHomeTemplateService;
import io.mango.infra.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/home/templates")
@RequiredArgsConstructor
@Tag(name = "首页管理", description = "平台管理员首页模板、发布和授权管理接口")
public class HomeTemplateController implements HomeTemplateApi {

    private final IHomeTemplateService homeTemplateService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:list")
    @Operation(summary = "查询首页模板", description = "权限接口。查询当前租户首页模板列表")
    public R<List<HomeTemplateVO>> list(@ParameterObject HomeTemplateQuery query) {
        return R.ok(homeTemplateService.list(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:query")
    @Operation(summary = "查询首页模板详情", description = "权限接口。查询首页模板草稿和发布版本信息")
    public R<HomeTemplateVO> detail(
            @Parameter(description = "模板ID") @RequestParam("id") Long id) {
        return R.ok(homeTemplateService.detail(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:add")
    @Operation(summary = "创建首页模板", description = "权限接口。创建首页模板草稿")
    @Log("创建首页模板")
    public R<HomeTemplateVO> create(@RequestBody CreateHomeTemplateCommand command) {
        return R.ok(homeTemplateService.create(command));
    }

    @Override
    @PutMapping("/draft")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:edit")
    @Operation(summary = "编辑首页模板草稿", description = "权限接口。只能编辑草稿版本")
    @Log("编辑首页模板草稿")
    public R<HomeTemplateVO> updateDraft(@RequestBody UpdateHomeTemplateDraftCommand command) {
        return R.ok(homeTemplateService.updateDraft(command));
    }

    @Override
    @PostMapping("/copy")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:add")
    @Operation(summary = "复制首页模板", description = "权限接口。复制模板生成新的草稿模板")
    @Log("复制首页模板")
    public R<HomeTemplateVO> copy(@RequestBody HomeTemplateIdCommand command) {
        return R.ok(homeTemplateService.copy(command));
    }

    @Override
    @PutMapping("/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:publish")
    @Operation(summary = "发布首页模板", description = "权限接口。发布草稿后授权用户看到最新发布版本")
    @Log("发布首页模板")
    public R<HomeTemplateVO> publish(@RequestBody HomeTemplateIdCommand command) {
        return R.ok(homeTemplateService.publish(command));
    }

    @Override
    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:status")
    @Operation(summary = "启停首页模板", description = "权限接口。停用后用户不再看到该授权首页")
    @Log("启停首页模板")
    public R<HomeTemplateVO> updateStatus(@RequestBody UpdateHomeTemplateStatusCommand command) {
        return R.ok(homeTemplateService.updateStatus(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:delete")
    @Operation(summary = "删除首页模板", description = "权限接口。仅允许删除未授权模板")
    @Log("删除首页模板")
    public R<Void> delete(@RequestBody HomeTemplateIdCommand command) {
        homeTemplateService.delete(command);
        return R.ok();
    }

    @Override
    @GetMapping("/authorizations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:auth")
    @Operation(summary = "查询首页模板授权", description = "权限接口。查询模板授权对象")
    public R<List<HomeTemplateAuthorizationVO>> listAuthorizations(
            @ParameterObject HomeTemplateAuthorizationQuery query) {
        return R.ok(homeTemplateService.listAuthorizations(query));
    }

    @Override
    @PutMapping("/authorizations")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:templates:auth")
    @Operation(summary = "保存首页模板授权", description = "权限接口。按个人、部门、角色保存模板授权")
    @Log("保存首页模板授权")
    public R<List<HomeTemplateAuthorizationVO>> saveAuthorizations(
            @RequestBody SaveHomeTemplateAuthorizationsCommand command) {
        return R.ok(homeTemplateService.saveAuthorizations(command));
    }

    @Override
    @GetMapping("/user-pages")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:user:view")
    @Operation(summary = "查询用户最终首页集合", description = "权限接口。查询用户自建和授权首页集合")
    public R<List<HomePageVO>> resolveUserPages(@ParameterObject UserHomeViewQuery query) {
        return R.ok(homeTemplateService.resolveUserPages(query));
    }
}
