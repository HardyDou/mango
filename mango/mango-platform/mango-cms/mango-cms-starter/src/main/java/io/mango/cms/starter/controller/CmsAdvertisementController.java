package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsAdvertisementApi;
import io.mango.cms.api.command.SaveCmsAdvertisementCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdvertisementPageQuery;
import io.mango.cms.api.vo.CmsAdvertisementVO;
import io.mango.cms.core.service.ICmsAdvertisementService;
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
 * CMS 广告位管理 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 广告位管理", description = "广告位管理接口")
public class CmsAdvertisementController implements CmsAdvertisementApi {

    private final ICmsAdvertisementService advertisementService;

    @Override
    @GetMapping("/advertisements/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:list")
    @Operation(summary = "分页查询广告位", description = "分页查询广告位")
    public R<PageResult<CmsAdvertisementVO>> pageAdvertisements(@ParameterObject CmsAdvertisementPageQuery query) {
        return R.ok(advertisementService.pageAdvertisements(query));
    }

    @Override
    @GetMapping("/advertisements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:query")
    @Operation(summary = "查询广告位详情", description = "查询广告位详情")
    public R<CmsAdvertisementVO> detailAdvertisement(
            @Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(advertisementService.detailAdvertisement(id));
    }

    @Override
    @PostMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:add")
    @Operation(summary = "创建广告位", description = "创建广告位")
    public R<Long> createAdvertisement(@RequestBody SaveCmsAdvertisementCommand command) {
        return R.ok(advertisementService.createAdvertisement(command));
    }

    @Override
    @PutMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:edit")
    @Operation(summary = "更新广告位", description = "更新广告位")
    public R<Boolean> updateAdvertisement(@RequestBody SaveCmsAdvertisementCommand command) {
        return R.ok(advertisementService.updateAdvertisement(command));
    }

    @Override
    @PutMapping("/advertisements/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:status")
    @Operation(summary = "更新广告位状态", description = "更新广告位状态")
    public R<Boolean> updateAdvertisementStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(advertisementService.updateAdvertisementStatus(command));
    }

    @Override
    @DeleteMapping("/advertisements")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:advertisement:delete")
    @Operation(summary = "删除广告位", description = "删除广告位")
    public R<Boolean> deleteAdvertisement(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(advertisementService.deleteAdvertisement(id));
    }
}
