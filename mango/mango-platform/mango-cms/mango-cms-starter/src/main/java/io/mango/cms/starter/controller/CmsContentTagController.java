package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsContentTagApi;
import io.mango.cms.api.command.SaveCmsContentTagCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsContentTagPageQuery;
import io.mango.cms.api.vo.CmsContentTagVO;
import io.mango.cms.core.service.ICmsContentTagService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
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

/**
 * CMS 内容标签 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 内容标签", description = "内容标签管理接口")
public class CmsContentTagController implements CmsContentTagApi {

    private final ICmsContentTagService contentTagService;

    @Override
    @GetMapping("/content-tags/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    @Operation(summary = "分页查询内容标签", description = "分页查询内容标签")
    public R<PageResult<CmsContentTagVO>> pageContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return R.ok(contentTagService.pageContentTags(query));
    }

    @Override
    @GetMapping("/content-tags/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:list")
    @Operation(summary = "查询内容标签列表", description = "查询内容标签列表")
    public R<List<CmsContentTagVO>> listContentTags(@ParameterObject CmsContentTagPageQuery query) {
        return R.ok(contentTagService.listContentTags(query));
    }

    @Override
    @GetMapping("/content-tags/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:query")
    @Operation(summary = "查询内容标签详情", description = "查询内容标签详情")
    public R<CmsContentTagVO> detailContentTag(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentTagService.detailContentTag(id));
    }

    @Override
    @PostMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:add")
    @Operation(summary = "创建内容标签", description = "创建内容标签")
    public R<Long> createContentTag(@RequestBody SaveCmsContentTagCommand command) {
        return R.ok(contentTagService.createContentTag(command));
    }

    @Override
    @PutMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:edit")
    @Operation(summary = "更新内容标签", description = "更新内容标签")
    public R<Boolean> updateContentTag(@RequestBody SaveCmsContentTagCommand command) {
        return R.ok(contentTagService.updateContentTag(command));
    }

    @Override
    @PutMapping("/content-tags/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:status")
    @Operation(summary = "更新内容标签状态", description = "更新内容标签状态")
    public R<Boolean> updateContentTagStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(contentTagService.updateContentTagStatus(command));
    }

    @Override
    @DeleteMapping("/content-tags")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content-tag:delete")
    @Operation(summary = "删除内容标签", description = "删除内容标签")
    public R<Boolean> deleteContentTag(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentTagService.deleteContentTag(id));
    }
}
