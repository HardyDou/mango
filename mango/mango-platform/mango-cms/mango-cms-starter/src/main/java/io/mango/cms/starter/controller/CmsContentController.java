package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsContentApi;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.command.SaveCmsContentCommand;
import io.mango.cms.api.command.UpdateCmsContentReviewCommand;
import io.mango.cms.api.query.CmsContentPageQuery;
import io.mango.cms.api.vo.CmsContentVO;
import io.mango.cms.core.service.ICmsContentService;
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

/**
 * CMS 内容管理 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 内容管理", description = "内容管理与审核接口")
public class CmsContentController implements CmsContentApi {

    private final ICmsContentService contentService;

    @Override
    @GetMapping("/contents/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:list")
    @Operation(summary = "分页查询内容", description = "分页查询内容")
    public R<PageResult<CmsContentVO>> pageContents(@ParameterObject CmsContentPageQuery query) {
        return R.ok(contentService.pageContents(query));
    }

    @Override
    @GetMapping("/contents/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:query")
    @Operation(summary = "查询内容详情", description = "查询内容详情")
    public R<CmsContentVO> detailContent(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentService.detailContent(id));
    }

    @Override
    @PostMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:add")
    @Operation(summary = "创建内容", description = "创建内容")
    public R<Long> createContent(@RequestBody SaveCmsContentCommand command) {
        return R.ok(contentService.createContent(command));
    }

    @Override
    @PutMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:edit")
    @Operation(summary = "更新内容", description = "更新内容")
    public R<Boolean> updateContent(@RequestBody SaveCmsContentCommand command) {
        return R.ok(contentService.updateContent(command));
    }

    @Override
    @PostMapping("/contents/submit")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:submit")
    @Operation(summary = "提交内容审核", description = "提交内容审核")
    public R<Boolean> submitContent(@RequestBody CmsOfflineCommand command) {
        return R.ok(contentService.submitContent(command));
    }

    @Override
    @PostMapping("/contents/approve")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:approve")
    @Operation(summary = "审核通过内容", description = "审核通过内容")
    public R<Boolean> approveContent(@RequestBody UpdateCmsContentReviewCommand command) {
        return R.ok(contentService.approveContent(command));
    }

    @Override
    @PostMapping("/contents/reject")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:reject")
    @Operation(summary = "驳回内容", description = "驳回内容")
    public R<Boolean> rejectContent(@RequestBody UpdateCmsContentReviewCommand command) {
        return R.ok(contentService.rejectContent(command));
    }

    @Override
    @PostMapping("/contents/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:offline")
    @Operation(summary = "下线内容", description = "下线内容")
    public R<Boolean> offlineContent(@RequestBody CmsOfflineCommand command) {
        return R.ok(contentService.offlineContent(command));
    }

    @Override
    @DeleteMapping("/contents")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:content:delete")
    @Operation(summary = "删除内容", description = "删除内容")
    public R<Boolean> deleteContent(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentService.deleteContent(id));
    }
}
