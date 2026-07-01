package io.mango.link.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.link.api.LinkAdminApi;
import io.mango.link.api.command.CreateLinkCategoryCommand;
import io.mango.link.api.command.CreateLinkItemCommand;
import io.mango.link.api.command.UpdateLinkCategoryCommand;
import io.mango.link.api.command.UpdateLinkCategoryStatusCommand;
import io.mango.link.api.command.UpdateLinkItemCommand;
import io.mango.link.api.command.UpdateLinkItemStatusCommand;
import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.query.LinkCategoryPageQuery;
import io.mango.link.api.query.LinkCategoryQuery;
import io.mango.link.api.query.LinkItemPageQuery;
import io.mango.link.api.vo.LinkCategoryVO;
import io.mango.link.api.vo.LinkItemVO;
import io.mango.link.core.service.ILinkAdminService;
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
@Tag(name = "网址管理", description = "维护网址分类、网址列表和可见范围")
public class LinkAdminController implements LinkAdminApi {

    private final ILinkAdminService linkAdminService;

    @Override
    @GetMapping("/categories/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:view")
    @Operation(summary = "分页查询网址分类")
    public R<PageResult<LinkCategoryVO>> pageCategories(@Valid @ParameterObject LinkCategoryPageQuery query) {
        return R.ok(linkAdminService.pageCategories(query));
    }

    @Override
    @GetMapping("/categories/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:view")
    @Operation(summary = "查询网址分类列表")
    public R<List<LinkCategoryVO>> listCategories(@Valid @ParameterObject LinkCategoryQuery query) {
        return R.ok(linkAdminService.listCategories(query));
    }

    @Override
    @PostMapping("/categories/create")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:create")
    @Operation(summary = "新增网址分类")
    public R<Long> createCategory(@Valid @RequestBody CreateLinkCategoryCommand command) {
        return R.ok(linkAdminService.createCategory(command));
    }

    @Override
    @PutMapping("/categories/update")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:update")
    @Operation(summary = "编辑网址分类")
    public R<Boolean> updateCategory(@Valid @RequestBody UpdateLinkCategoryCommand command) {
        return R.ok(linkAdminService.updateCategory(command));
    }

    @Override
    @PostMapping("/categories/enable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:status")
    @Operation(summary = "启用网址分类")
    public R<Boolean> enableCategory(
            @Parameter(description = "分类 ID", required = true)
            @NotNull(message = "分类 ID 不能为空")
            @RequestParam Long id) {
        UpdateLinkCategoryStatusCommand command = new UpdateLinkCategoryStatusCommand();
        command.setId(id);
        command.setStatus(LinkStatus.ENABLED);
        return R.ok(linkAdminService.updateCategoryStatus(command));
    }

    @Override
    @PostMapping("/categories/disable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:status")
    @Operation(summary = "停用网址分类")
    public R<Boolean> disableCategory(
            @Parameter(description = "分类 ID", required = true)
            @NotNull(message = "分类 ID 不能为空")
            @RequestParam Long id) {
        UpdateLinkCategoryStatusCommand command = new UpdateLinkCategoryStatusCommand();
        command.setId(id);
        command.setStatus(LinkStatus.DISABLED);
        return R.ok(linkAdminService.updateCategoryStatus(command));
    }

    @Override
    @PutMapping("/categories/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:status")
    @Operation(summary = "更新网址分类状态")
    public R<Boolean> updateCategoryStatus(@Valid @RequestBody UpdateLinkCategoryStatusCommand command) {
        return R.ok(linkAdminService.updateCategoryStatus(command));
    }

    @Override
    @DeleteMapping("/categories/delete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:category:delete")
    @Operation(summary = "删除网址分类")
    public R<Boolean> deleteCategory(
            @Parameter(description = "分类 ID", required = true)
            @NotNull(message = "分类 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(linkAdminService.deleteCategory(id));
    }

    @Override
    @GetMapping("/items/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:view")
    @Operation(summary = "分页查询网址列表")
    public R<PageResult<LinkItemVO>> pageItems(@Valid @ParameterObject LinkItemPageQuery query) {
        return R.ok(linkAdminService.pageItems(query));
    }

    @Override
    @PostMapping("/items/create")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:create")
    @Operation(summary = "新增网址")
    public R<Long> createItem(@Valid @RequestBody CreateLinkItemCommand command) {
        return R.ok(linkAdminService.createItem(command));
    }

    @Override
    @PutMapping("/items/update")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:update")
    @Operation(summary = "编辑网址")
    public R<Boolean> updateItem(@Valid @RequestBody UpdateLinkItemCommand command) {
        return R.ok(linkAdminService.updateItem(command));
    }

    @Override
    @PostMapping("/items/enable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:status")
    @Operation(summary = "启用网址")
    public R<Boolean> enableItem(
            @Parameter(description = "网址 ID", required = true)
            @NotNull(message = "网址 ID 不能为空")
            @RequestParam Long id) {
        UpdateLinkItemStatusCommand command = new UpdateLinkItemStatusCommand();
        command.setId(id);
        command.setStatus(LinkStatus.ENABLED);
        return R.ok(linkAdminService.updateItemStatus(command));
    }

    @Override
    @PostMapping("/items/disable")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:status")
    @Operation(summary = "停用网址")
    public R<Boolean> disableItem(
            @Parameter(description = "网址 ID", required = true)
            @NotNull(message = "网址 ID 不能为空")
            @RequestParam Long id) {
        UpdateLinkItemStatusCommand command = new UpdateLinkItemStatusCommand();
        command.setId(id);
        command.setStatus(LinkStatus.DISABLED);
        return R.ok(linkAdminService.updateItemStatus(command));
    }

    @Override
    @PutMapping("/items/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:status")
    @Operation(summary = "更新网址状态")
    public R<Boolean> updateItemStatus(@Valid @RequestBody UpdateLinkItemStatusCommand command) {
        return R.ok(linkAdminService.updateItemStatus(command));
    }

    @Override
    @DeleteMapping("/items/delete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "link:item:delete")
    @Operation(summary = "删除网址")
    public R<Boolean> deleteItem(
            @Parameter(description = "网址 ID", required = true)
            @NotNull(message = "网址 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(linkAdminService.deleteItem(id));
    }
}
