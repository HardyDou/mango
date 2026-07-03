package io.mango.link.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.link.api.LinkUserApi;
import io.mango.link.api.command.CreateLinkFavoriteCommand;
import io.mango.link.api.command.CreateLinkPersonalCategoryCommand;
import io.mango.link.api.command.CreateLinkPersonalItemCommand;
import io.mango.link.api.command.DeleteLinkFavoriteCommand;
import io.mango.link.api.command.UpdateLinkPersonalCategoryCommand;
import io.mango.link.api.command.UpdateLinkPersonalItemCommand;
import io.mango.link.api.query.LinkCompanyItemQuery;
import io.mango.link.api.query.LinkFavoriteQuery;
import io.mango.link.api.query.LinkPersonalItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkNavigationWidgetDataVO;
import io.mango.link.api.vo.LinkPersonalItemVO;
import io.mango.link.core.service.ILinkUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
@Validated
@Tag(name = "网址导航", description = "用户侧网址查询、收藏和个人网址")
public class LinkUserController implements LinkUserApi {

    private final ILinkUserService linkUserService;

    @Override
    @GetMapping("/company-links/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询公司网址")
    @Operation(summary = "查询公司网址")
    public R<List<LinkNavigationItemVO>> listCompanyItems(@Valid @ParameterObject LinkCompanyItemQuery query) {
        return R.ok(linkUserService.listCompanyItems(query));
    }

    @Override
    @GetMapping("/navigation-widget/data")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询首页网址导航小组件数据")
    @Operation(summary = "查询首页网址导航小组件数据")
    public R<LinkNavigationWidgetDataVO> getNavigationWidgetData() {
        return R.ok(linkUserService.getNavigationWidgetData());
    }

    @Override
    @GetMapping("/personal-categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "查询我的网址分组")
    public R<List<LinkCategoryVO>> listPersonalCategories() {
        return R.ok(linkUserService.listPersonalCategories());
    }

    @Override
    @PostMapping("/personal-categories/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "新增我的网址分组")
    public R<Long> createPersonalCategory(@Valid @RequestBody CreateLinkPersonalCategoryCommand command) {
        return R.ok(linkUserService.createPersonalCategory(command));
    }

    @Override
    @PutMapping("/personal-categories/update")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "编辑我的网址分组")
    public R<Boolean> updatePersonalCategory(@Valid @RequestBody UpdateLinkPersonalCategoryCommand command) {
        return R.ok(linkUserService.updatePersonalCategory(command));
    }

    @Override
    @DeleteMapping("/personal-categories/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "删除我的网址分组")
    public R<Boolean> deletePersonalCategory(
            @Parameter(description = "分组 ID", required = true)
            @NotNull(message = "分组 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(linkUserService.deletePersonalCategory(id));
    }

    @Override
    @PostMapping("/favorites/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "收藏网址")
    public R<Boolean> createFavorite(@Valid @RequestBody CreateLinkFavoriteCommand command) {
        return R.ok(linkUserService.createFavorite(command));
    }

    @Override
    @DeleteMapping("/favorites/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "取消收藏")
    public R<Boolean> deleteFavorite(@Valid @RequestBody DeleteLinkFavoriteCommand command) {
        return R.ok(linkUserService.deleteFavorite(command));
    }

    @Override
    @GetMapping("/favorites/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "查询我的收藏")
    public R<List<LinkFavoriteVO>> listFavorites(@Valid @ParameterObject LinkFavoriteQuery query) {
        return R.ok(linkUserService.listFavorites(query));
    }

    @Override
    @GetMapping("/personal-links/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "分页查询我的网址")
    public R<PageResult<LinkPersonalItemVO>> pagePersonalItems(@Valid @ParameterObject LinkPersonalItemPageQuery query) {
        return R.ok(linkUserService.pagePersonalItems(query));
    }

    @Override
    @PostMapping("/personal-links/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "新增我的网址")
    public R<Long> createPersonalItem(@Valid @RequestBody CreateLinkPersonalItemCommand command) {
        return R.ok(linkUserService.createPersonalItem(command));
    }

    @Override
    @PutMapping("/personal-links/update")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "编辑我的网址")
    public R<Boolean> updatePersonalItem(@Valid @RequestBody UpdateLinkPersonalItemCommand command) {
        return R.ok(linkUserService.updatePersonalItem(command));
    }

    @Override
    @DeleteMapping("/personal-links/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "删除我的网址")
    public R<Boolean> deletePersonalItem(
            @Parameter(description = "网址 ID", required = true)
            @NotNull(message = "网址 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(linkUserService.deletePersonalItem(id));
    }
}
