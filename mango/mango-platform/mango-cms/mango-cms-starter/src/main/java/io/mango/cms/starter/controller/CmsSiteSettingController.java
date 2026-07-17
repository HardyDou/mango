package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsSiteSettingApi;
import io.mango.cms.api.command.SaveCmsSiteSettingCommand;
import io.mango.cms.api.vo.CmsSiteSettingVO;
import io.mango.cms.core.service.ICmsSiteSettingService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CMS 站点设置 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/cms")
@RequiredArgsConstructor
@Tag(name = "CMS 站点设置", description = "站点设置管理接口")
public class CmsSiteSettingController implements CmsSiteSettingApi {

    private final ICmsSiteSettingService siteSettingService;

    @Override
    @GetMapping("/site-settings/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:query")
    @Operation(summary = "查询站点设置详情", description = "查询站点设置详情")
    public R<CmsSiteSettingVO> detailSiteSetting(
            @Parameter(description = "站点 ID") @RequestParam("siteId") Long siteId) {
        return R.ok(siteSettingService.detailSiteSetting(siteId));
    }

    @Override
    @PutMapping("/site-settings")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "cms:site-setting:edit")
    @Operation(summary = "保存站点设置", description = "保存站点设置")
    public R<Boolean> saveSiteSetting(@RequestBody SaveCmsSiteSettingCommand command) {
        return R.ok(siteSettingService.saveSiteSetting(command));
    }
}
