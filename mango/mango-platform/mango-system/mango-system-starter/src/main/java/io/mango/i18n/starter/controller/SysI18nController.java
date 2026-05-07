package io.mango.i18n.starter.controller;

import io.mango.common.result.R;
import io.mango.authorization.api.annotation.PublicApi;
import io.mango.i18n.api.SysI18nApi;
import io.mango.i18n.api.entity.SysI18n;
import io.mango.i18n.core.service.ISysI18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * i18n controller - implements SysI18nApi
 *
 * @author Mango
 */
@RestController
@RequestMapping("/system/i18n")
@RequiredArgsConstructor
@Tag(name = "国际化", description = "国际化语言包与语言列表接口")
public class SysI18nController implements SysI18nApi {

    private final ISysI18nService sysI18nService;

    @Override
    public Map<String, List<Map<String, String>>> listMap() {
        return sysI18nService.listMap();
    }

    @Override
    public List<Map<String, String>> listByLang(String lang) {
        return sysI18nService.listByLang(lang);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return sysI18nService.getSupportedLanguages();
    }

    @Override
    public SysI18n getByName(String name) {
        return sysI18nService.getByName(name);
    }

    /**
     * Public endpoint: Get all i18n entries grouped by language
     * No authentication required
     */
    @GetMapping("/public")
    @PublicApi(desc = "获取公开国际化语言包")
    @Operation(summary = "获取公开国际化语言包", description = "公开接口。获取所有语言的公开国际化语言包")
    public R<Map<String, List<Map<String, String>>>> publicInfo() {
        return R.ok(listMap());
    }

    /**
     * Public endpoint: Get i18n entries for a specific language
     * No authentication required
     */
    @GetMapping("/public/lang")
    @PublicApi(desc = "按语言获取公开国际化语言包")
    @Operation(summary = "按语言获取公开国际化语言包", description = "公开接口。按语言编码获取公开国际化语言包")
    public R<List<Map<String, String>>> publicInfoByLang(
            @Parameter(description = "语言编码，例如 zh_CN、en")
            @RequestParam(value = "lang") String lang) {
        return R.ok(listByLang(lang));
    }

    /**
     * Public endpoint: Get supported languages
     * No authentication required
     */
    @GetMapping("/languages")
    @PublicApi(desc = "获取公开支持语言列表")
    @Operation(summary = "获取支持语言列表", description = "公开接口。获取当前支持的语言编码列表")
    public R<List<String>> languages() {
        return R.ok(getSupportedLanguages());
    }

    /**
     * Public endpoint: Get i18n entry by key
     * No authentication required
     */
    @GetMapping("/public/name")
    @PublicApi(desc = "按键名获取公开国际化条目")
    @Operation(summary = "按键名获取国际化条目", description = "公开接口。按国际化键名查询国际化条目")
    public R<SysI18n> getByNameEndpoint(
            @Parameter(description = "国际化键名")
            @RequestParam(value = "name") String name) {
        SysI18n result = getByName(name);
        if (result == null) {
            return R.fail(404, "I18n entry not found");
        }
        return R.ok(result);
    }

    /**
     * Frontend-compatible endpoint: Get i18n entries for a specific language
     * Returns {lang: [...]} format to match frontend fetchI18n(false) usage
     * No authentication required
     */
    @GetMapping
    @PublicApi(desc = "按语言获取前端国际化语言包")
    @Operation(summary = "获取前端国际化语言包", description = "公开接口。按语言编码获取前端兼容格式的国际化语言包")
    public R<Map<String, List<Map<String, String>>>> i18n(
            @Parameter(description = "语言编码，例如 zh_CN、en")
            @RequestParam(value = "lang") String lang) {
        // 返回 {lang: [...]} 格式，匹配前端 fetchI18n(false) 的 res.data[lang] 访问方式
        Map<String, List<Map<String, String>>> result = Map.of(lang, sysI18nService.listByLang(lang));
        return R.ok(result);
    }
}
