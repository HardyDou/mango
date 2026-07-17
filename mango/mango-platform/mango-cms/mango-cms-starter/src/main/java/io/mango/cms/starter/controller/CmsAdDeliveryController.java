package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsAdDeliveryApi;
import io.mango.cms.api.command.SaveCmsAdDeliveryCommand;
import io.mango.cms.api.command.UpdateCmsStatusCommand;
import io.mango.cms.api.query.CmsAdDeliveryPageQuery;
import io.mango.cms.api.vo.CmsAdDeliveryVO;
import io.mango.cms.core.service.ICmsAdDeliveryService;
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
 * CMS 广告投放 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 广告投放", description = "广告投放管理接口")
public class CmsAdDeliveryController implements CmsAdDeliveryApi {

    private final ICmsAdDeliveryService adDeliveryService;

    @Override
    @GetMapping("/ad-deliveries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:list")
    @Operation(summary = "分页查询广告投放", description = "分页查询广告投放")
    public R<PageResult<CmsAdDeliveryVO>> pageAdDeliveries(@ParameterObject CmsAdDeliveryPageQuery query) {
        return R.ok(adDeliveryService.pageAdDeliveries(query));
    }

    @Override
    @GetMapping("/ad-deliveries/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:query")
    @Operation(summary = "查询广告投放详情", description = "查询广告投放详情")
    public R<CmsAdDeliveryVO> detailAdDelivery(
            @Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(adDeliveryService.detailAdDelivery(id));
    }

    @Override
    @PostMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:add")
    @Operation(summary = "创建广告投放", description = "创建广告投放")
    public R<Long> createAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command) {
        return R.ok(adDeliveryService.createAdDelivery(command));
    }

    @Override
    @PutMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:edit")
    @Operation(summary = "更新广告投放", description = "更新广告投放")
    public R<Boolean> updateAdDelivery(@RequestBody SaveCmsAdDeliveryCommand command) {
        return R.ok(adDeliveryService.updateAdDelivery(command));
    }

    @Override
    @PutMapping("/ad-deliveries/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:status")
    @Operation(summary = "更新广告投放状态", description = "更新广告投放状态")
    public R<Boolean> updateAdDeliveryStatus(@RequestBody UpdateCmsStatusCommand command) {
        return R.ok(adDeliveryService.updateAdDeliveryStatus(command));
    }

    @Override
    @DeleteMapping("/ad-deliveries")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:ad-delivery:delete")
    @Operation(summary = "删除广告投放", description = "删除广告投放")
    public R<Boolean> deleteAdDelivery(@Parameter(description = "主键 ID") @RequestParam("id") Long id) {
        return R.ok(adDeliveryService.deleteAdDelivery(id));
    }
}
