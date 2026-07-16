package io.mango.org.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.org.api.PostApi;
import io.mango.org.api.command.CreatePostCommand;
import io.mango.org.api.command.UpdatePostCommand;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.core.service.IPostService;
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

/**
 * 岗位管理 HTTP 适配器。
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Validated
@Tag(name = "岗位管理", description = "岗位分页、详情、新增、修改与删除接口")
public class PostController implements PostApi {

    private final IPostService postService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:list")
    @Operation(summary = "分页查询岗位", description = "按岗位名称、编码和状态分页查询岗位")
    public R<PageResult<PostVO>> page(@ParameterObject PostPageQuery query) {
        return R.ok(postService.pageResult(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:query")
    @Operation(summary = "获取岗位详情", description = "按岗位ID查询岗位详情")
    public R<PostVO> get(
            @Parameter(description = "岗位ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(postService.detail(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:add")
    @Operation(summary = "新增岗位", description = "在当前租户内创建岗位")
    public R<Long> save(@RequestBody CreatePostCommand command) {
        return R.ok(postService.create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:edit")
    @Operation(summary = "修改岗位", description = "更新当前租户内的岗位")
    public R<Boolean> update(@RequestBody UpdatePostCommand command) {
        return R.ok(postService.update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:post:delete")
    @Operation(summary = "删除岗位", description = "按岗位ID删除岗位")
    public R<Boolean> delete(
            @Parameter(description = "岗位ID", required = true)
            @RequestParam("id") Long id) {
        DeleteCommand command = new DeleteCommand();
        command.setId(id);
        return R.ok(postService.delete(command));
    }
}
