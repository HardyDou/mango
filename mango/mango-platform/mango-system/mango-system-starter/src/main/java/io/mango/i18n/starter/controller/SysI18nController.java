package io.mango.i18n.starter.controller;

import io.mango.authorization.api.annotation.PublicAccess;
import io.mango.common.result.R;
import io.mango.i18n.api.SysI18nApi;
import io.mango.i18n.api.vo.SysI18nMessageVO;
import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import io.mango.i18n.core.service.ISysI18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/system/i18n")
@RequiredArgsConstructor
@Tag(name = "国际化", description = "国际化语言包与语言列表接口")
public class SysI18nController implements SysI18nApi {

    private final ISysI18nService i18nService;

    @Override
    @GetMapping("/public")
    @PublicAccess(desc = "获取公开国际化语言包")
    @Operation(summary = "获取公开国际化语言包", description = "获取公开国际化语言包并返回处理结果")
    public R<I18nLanguagePackVO> publicInfo() {
        return R.ok(i18nService.listMap());
    }

    @Override
    @GetMapping("/public/lang")
    @PublicAccess(desc = "按语言获取公开国际化语言包")
    @Operation(summary = "按语言获取公开国际化语言包", description = "按语言获取公开国际化语言包并返回处理结果")
    public R<List<I18nEntryVO>> publicInfoByLang(@Parameter(description = "语言编码", required = true) @RequestParam("lang") String lang) {
        return R.ok(i18nService.listByLang(lang));
    }

    @Override
    @GetMapping("/languages")
    @PublicAccess(desc = "获取公开支持语言列表")
    @Operation(summary = "获取支持语言列表", description = "获取支持语言列表并返回处理结果")
    public R<List<String>> languages() {
        return R.ok(i18nService.getSupportedLanguages());
    }

    @Override
    @GetMapping("/public/name")
    @PublicAccess(desc = "按键名获取公开国际化条目")
    @Operation(summary = "按键名获取国际化条目", description = "按键名获取国际化条目并返回处理结果")
    public R<SysI18nMessageVO> getByName(@Parameter(description = "国际化键名", required = true) @RequestParam("name") String name) {
        return R.ok(i18nService.getByName(name));
    }

    @Override
    @GetMapping
    @PublicAccess(desc = "按语言获取前端国际化语言包")
    @Operation(summary = "获取前端国际化语言包", description = "获取前端国际化语言包并返回处理结果")
    public R<I18nLanguagePackVO> i18n(@Parameter(description = "语言编码", required = true) @RequestParam("lang") String lang) {
        return R.ok(i18nService.languagePack(lang));
    }
}
