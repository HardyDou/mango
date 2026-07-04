package io.mango.home.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.home.api.HomePageApi;
import io.mango.home.api.command.BatchDeleteHomePagesCommand;
import io.mango.home.api.command.CreateHomePageCommand;
import io.mango.home.api.command.HomePageIdCommand;
import io.mango.home.api.command.RenameHomePageCommand;
import io.mango.home.api.command.SaveHomePageLayoutCommand;
import io.mango.home.api.command.SetDefaultHomePageCommand;
import io.mango.home.api.command.SortHomePagesCommand;
import io.mango.home.api.query.ResolveHomePageQuery;
import io.mango.home.api.query.UserHomePageQuery;
import io.mango.home.api.vo.HomePageVO;
import io.mango.home.core.service.IHomePageService;
import io.mango.common.vo.PageResult;
import io.mango.infra.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/home/pages")
@RequiredArgsConstructor
@Tag(name = "首页工作台", description = "当前登录用户多首页与默认首页接口")
public class HomePageController implements HomePageApi {

    private final IHomePageService homePageService;

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询我的首页")
    @Operation(summary = "查询我的首页", description = "登录接口。查询当前用户拥有的首页列表")
    public R<List<HomePageVO>> listMyPages() {
        return R.ok(homePageService.listMyPages());
    }

    @Override
    @GetMapping("/user-pages")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:view")
    @Operation(summary = "分页查询用户自定义首页", description = "后台接口。分页查询当前租户下所有用户自定义首页")
    public R<PageResult<HomePageVO>> pageUserPages(@Valid @ParameterObject UserHomePageQuery query) {
        return R.ok(homePageService.pageUserPages(query));
    }

    @Override
    @GetMapping("/resolve")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "解析默认首页")
    @Operation(summary = "解析默认首页", description = "登录接口。解析当前用户默认首页或指定首页")
    public R<HomePageVO> resolve(@Valid @ParameterObject ResolveHomePageQuery query) {
        return R.ok(homePageService.resolve(query));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "创建首页")
    @Operation(summary = "创建首页", description = "登录接口。为当前用户创建一个首页")
    @Log("创建首页")
    public R<HomePageVO> create(@RequestBody @Valid CreateHomePageCommand command) {
        return R.ok(homePageService.create(command));
    }

    @Override
    @PutMapping("/name")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "重命名首页")
    @Operation(summary = "重命名首页", description = "登录接口。重命名当前用户拥有的首页")
    @Log("重命名首页")
    public R<HomePageVO> rename(@RequestBody @Valid RenameHomePageCommand command) {
        return R.ok(homePageService.rename(command.getId(), command));
    }

    @Override
    @PostMapping("/duplicate")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "复制首页")
    @Operation(summary = "复制首页", description = "登录接口。复制当前用户拥有的首页")
    @Log("复制首页")
    public R<HomePageVO> duplicate(@RequestBody @Valid HomePageIdCommand command) {
        return R.ok(homePageService.duplicate(command.getId()));
    }

    @Override
    @PutMapping("/layout")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "保存首页布局")
    @Operation(summary = "保存首页布局", description = "登录接口。保存当前用户指定首页的布局")
    @Log("保存首页布局")
    public R<HomePageVO> saveLayout(@RequestBody @Valid SaveHomePageLayoutCommand command) {
        return R.ok(homePageService.saveLayout(command.getId(), command));
    }

    @Override
    @PutMapping("/sort")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "首页排序")
    @Operation(summary = "首页排序", description = "登录接口。按当前用户提交的 ID 顺序更新首页排序")
    @Log("首页排序")
    public R<List<HomePageVO>> sort(@RequestBody @Valid SortHomePagesCommand command) {
        return R.ok(homePageService.sort(command));
    }

    @Override
    @PutMapping("/default")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "设置默认首页")
    @Operation(summary = "设置默认首页", description = "登录接口。设置当前用户默认首页")
    @Log("设置默认首页")
    public R<HomePageVO> setDefault(@RequestBody @Valid SetDefaultHomePageCommand command) {
        return R.ok(homePageService.setDefault(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "删除首页")
    @Operation(summary = "删除首页", description = "登录接口。删除当前用户拥有的首页")
    @Log("删除首页")
    public R<HomePageVO> delete(@RequestBody @Valid HomePageIdCommand command) {
        return R.ok(homePageService.delete(command.getId()));
    }

    @Override
    @PutMapping("/admin/name")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:edit")
    @Operation(summary = "后台重命名用户首页", description = "后台接口。重命名当前租户下指定用户首页")
    @Log("后台重命名用户首页")
    public R<HomePageVO> adminRename(@RequestBody @Valid RenameHomePageCommand command) {
        return R.ok(homePageService.adminRename(command.getId(), command));
    }

    @Override
    @PutMapping("/admin/layout")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:edit")
    @Operation(summary = "后台保存用户首页布局", description = "后台接口。保存当前租户下指定用户首页布局")
    @Log("后台保存用户首页布局")
    public R<HomePageVO> adminSaveLayout(@RequestBody @Valid SaveHomePageLayoutCommand command) {
        return R.ok(homePageService.adminSaveLayout(command.getId(), command));
    }

    @Override
    @DeleteMapping("/admin")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:delete")
    @Operation(summary = "后台删除用户首页", description = "后台接口。删除当前租户下指定用户首页")
    @Log("后台删除用户首页")
    public R<Void> adminDelete(@RequestBody @Valid HomePageIdCommand command) {
        homePageService.adminDelete(command.getId());
        return R.ok();
    }

    @Override
    @DeleteMapping("/admin/batch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "home:list:delete")
    @Operation(summary = "后台批量删除用户首页", description = "后台接口。批量删除当前租户下指定用户首页")
    @Log("后台批量删除用户首页")
    public R<Void> adminBatchDelete(@RequestBody @Valid BatchDeleteHomePagesCommand command) {
        homePageService.adminBatchDelete(command);
        return R.ok();
    }
}
