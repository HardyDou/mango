package io.mango.bff.admin.core.controller;

import io.mango.common.result.R;
import io.mango.i18n.api.service.SysI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * BFF i18n controller - aggregates i18n data for frontend
 *
 * @author Mango
 */
@RestController
@RequestMapping("/bff/admin/i18n")
public class I18nBffController {

    @Autowired
    private SysI18nService sysI18nService;

    /**
     * Get i18n entries for a specific language
     *
     * @param lang language code (e.g., "zh-cn", "en")
     * @return list of i18n key-value pairs
     */
    @GetMapping
    public R<List<Map<String, String>>> i18n(@RequestParam String lang) {
        return R.ok(sysI18nService.listByLang(lang));
    }

    /**
     * Get all i18n entries grouped by language
     *
     * @return map of language -> i18n entries
     */
    @GetMapping("/all")
    public R<Map<String, List<Map<String, String>>>> all() {
        return R.ok(sysI18nService.listMap());
    }

    /**
     * Get supported languages
     *
     * @return list of supported language codes
     */
    @GetMapping("/languages")
    public R<List<String>> languages() {
        return R.ok(sysI18nService.getSupportedLanguages());
    }
}
