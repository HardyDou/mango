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
import io.mango.link.api.query.LinkPublicItemQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkFavoriteVO;
import io.mango.link.api.vo.LinkNavigationItemVO;
import io.mango.link.api.vo.LinkNavigationWidgetDataVO;
import io.mango.link.api.vo.LinkPersonalItemVO;
import io.mango.link.api.vo.LinkPublicItemVO;
import io.mango.link.core.service.ILinkOpenService;
import io.mango.link.core.service.ILinkUserService;
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

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
@Validated
@Tag(name = "网址导航", description = "用户侧网址查询、收藏和个人网址")
public class LinkUserController implements LinkUserApi {

    private final ILinkUserService linkUserService;
    private final ILinkOpenService linkOpenService;

    @Override
    @GetMapping("/company-links/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询公司网址")
    @Operation(summary = "查询公司网址", description = "查询当前用户在所属租户内可见的公司网址")
    public R<List<LinkNavigationItemVO>> listCompanyItems(@ParameterObject LinkCompanyItemQuery query) {
        return R.ok(linkUserService.listCompanyItems(query));
    }

    @Override
    @GetMapping("/visible-links/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前用户可见网址")
    @Operation(summary = "查询可见网址", description = "查询当前用户可见的企业、收藏和个人网址")
    public R<List<LinkPublicItemVO>> listVisibleItems(@ParameterObject LinkPublicItemQuery query) {
        return R.ok(linkOpenService.listVisibleItems(query));
    }

    @Override
    @GetMapping("/navigation-widget/data")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询首页网址导航小组件数据")
    @Operation(summary = "查询首页网址导航小组件数据", description = "一次查询公司网址、个人网址、收藏和个人分组")
    public R<LinkNavigationWidgetDataVO> getNavigationWidgetData() {
        return R.ok(linkUserService.getNavigationWidgetData());
    }

    @Override
    @GetMapping("/personal-categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "查询我的网址分组", description = "查询当前用户已启用的个人网址分组")
    public R<List<LinkCategoryVO>> listPersonalCategories() {
        return R.ok(linkUserService.listPersonalCategories());
    }

    @Override
    @PostMapping("/personal-categories/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "新增我的网址分组", description = "为当前用户新增个人网址分组")
    public R<Long> createPersonalCategory(@RequestBody CreateLinkPersonalCategoryCommand command) {
        return R.ok(linkUserService.createPersonalCategory(command));
    }

    @Override
    @PutMapping("/personal-categories/update")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "编辑我的网址分组", description = "编辑当前用户拥有的个人网址分组")
    public R<Boolean> updatePersonalCategory(@RequestBody UpdateLinkPersonalCategoryCommand command) {
        return R.ok(linkUserService.updatePersonalCategory(command));
    }

    @Override
    @DeleteMapping("/personal-categories/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "删除我的网址分组", description = "删除当前用户拥有且未关联网址的个人分组")
    public R<Boolean> deletePersonalCategory(
            @Parameter(description = "分组 ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(linkUserService.deletePersonalCategory(id));
    }

    @Override
    @PostMapping("/favorites/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "收藏网址", description = "将当前用户可见的网址加入个人收藏")
    public R<Boolean> createFavorite(@RequestBody CreateLinkFavoriteCommand command) {
        return R.ok(linkUserService.createFavorite(command));
    }

    @Override
    @DeleteMapping("/favorites/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "取消收藏", description = "取消当前用户对指定网址的收藏")
    public R<Boolean> deleteFavorite(@RequestBody DeleteLinkFavoriteCommand command) {
        return R.ok(linkUserService.deleteFavorite(command));
    }

    @Override
    @GetMapping("/favorites/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "查询我的收藏", description = "查询当前用户仍然可见的收藏网址")
    public R<List<LinkFavoriteVO>> listFavorites(@ParameterObject LinkFavoriteQuery query) {
        return R.ok(linkUserService.listFavorites(query));
    }

    @Override
    @GetMapping("/personal-links/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "分页查询我的网址", description = "分页查询当前用户维护的个人网址")
    public R<PageResult<LinkPersonalItemVO>> pagePersonalItems(@ParameterObject LinkPersonalItemPageQuery query) {
        return R.ok(linkUserService.pagePersonalItems(query));
    }

    @Override
    @PostMapping("/personal-links/create")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "新增我的网址", description = "在当前用户的个人分组中新增网址")
    public R<Long> createPersonalItem(@RequestBody CreateLinkPersonalItemCommand command) {
        return R.ok(linkUserService.createPersonalItem(command));
    }

    @Override
    @PutMapping("/personal-links/update")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "编辑我的网址", description = "编辑当前用户拥有的个人网址")
    public R<Boolean> updatePersonalItem(@RequestBody UpdateLinkPersonalItemCommand command) {
        return R.ok(linkUserService.updatePersonalItem(command));
    }

    @Override
    @DeleteMapping("/personal-links/delete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "删除我的网址", description = "删除当前用户拥有的个人网址及其收藏关系")
    public R<Boolean> deletePersonalItem(
            @Parameter(description = "网址 ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(linkUserService.deletePersonalItem(id));
    }
}
