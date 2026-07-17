package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsContentPublishApi;
import io.mango.cms.api.command.BatchCmsContentPublishCommand;
import io.mango.cms.api.command.CmsOfflineCommand;
import io.mango.cms.api.query.CmsContentPublishPageQuery;
import io.mango.cms.api.vo.CmsContentPublishVO;
import io.mango.cms.core.service.ICmsContentPublishService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CMS 内容发布 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 内容发布", description = "内容发布关系管理接口")
public class CmsContentPublishController implements CmsContentPublishApi {

    private final ICmsContentPublishService contentPublishService;

    @Override
    @GetMapping("/content-publishes/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:list")
    @Operation(summary = "分页查询发布关系", description = "分页查询发布关系")
    public R<PageResult<CmsContentPublishVO>> pagePublishes(@ParameterObject CmsContentPublishPageQuery query) {
        return R.ok(contentPublishService.pagePublishes(query));
    }

    @Override
    @PostMapping("/content-publishes/publish")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:publish")
    @Operation(summary = "发布内容", description = "发布内容")
    public R<Boolean> publishContents(@RequestBody BatchCmsContentPublishCommand command) {
        return R.ok(contentPublishService.publishContents(command));
    }

    @Override
    @PostMapping("/content-publishes/offline")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:offline")
    @Operation(summary = "下线发布关系", description = "下线发布关系")
    public R<Boolean> offlinePublish(@RequestBody CmsOfflineCommand command) {
        return R.ok(contentPublishService.offlinePublish(command));
    }

    @Override
    @DeleteMapping("/content-publishes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:publish:delete")
    @Operation(summary = "删除发布关系", description = "删除发布关系")
    public R<Boolean> deletePublish(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(contentPublishService.deletePublish(id));
    }
}
