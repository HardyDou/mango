package io.mango.org.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.org.api.PostApi;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.service.IPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Tag(name = "岗位管理", description = "岗位分页、详情、新增、修改与删除接口")
public class PostController implements PostApi {

    private final IPostService postService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:list")
    @Operation(summary = "分页查询岗位", description = "权限接口。按分页条件查询岗位列表")
    public R<PageResult<PostVO>> page(@ParameterObject PostPageQuery query) {
        return R.ok(postService.page(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:query")
    @Operation(summary = "获取岗位详情", description = "权限接口。按岗位ID查询岗位详情")
    public R<PostVO> get(
            @Parameter(description = "岗位ID")
            @RequestParam Long id) {
        return R.ok(postService.getById(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:add")
    @Operation(summary = "新增岗位", description = "权限接口。创建岗位")
    public R<Void> save(@Valid @RequestBody CreatePostCommand command) {
        postService.save(command);
        return R.ok();
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:edit")
    @Operation(summary = "修改岗位", description = "权限接口。更新岗位")
    public R<Void> update(@Valid @RequestBody UpdatePostCommand command) {
        postService.update(command);
        return R.ok();
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:delete")
    @Operation(summary = "删除岗位", description = "权限接口。按岗位ID删除岗位")
    public R<Void> delete(
            @Parameter(description = "岗位ID")
            @RequestParam Long id) {
        postService.delete(id);
        return R.ok();
    }
}
