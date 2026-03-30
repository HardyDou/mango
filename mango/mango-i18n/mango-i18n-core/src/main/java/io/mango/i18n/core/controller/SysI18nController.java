package io.mango.i18n.core.controller;

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
 * i18n controller
 *
 * @author Mango
 */
@RestController
@RequestMapping("/i18n")
public class SysI18nController {

    @Autowired
    private SysI18nService sysI18nService;

    /**
     * Public endpoint: Get all i18n entries grouped by language
     * No authentication required
     */
    @GetMapping("/public")
    public R<Map<String, List<Map<String, String>>>> publicInfo() {
        return R.ok(sysI18nService.listMap());
    }

    /**
     * Public endpoint: Get i18n entries for a specific language
     * No authentication required
     */
    @GetMapping("/public/lang")
    public R<List<Map<String, String>>> publicInfoByLang(@RequestParam String lang) {
        return R.ok(sysI18nService.listByLang(lang));
    }

    /**
     * Public endpoint: Get supported languages
     * No authentication required
     */
    @GetMapping("/languages")
    public R<List<String>> languages() {
        return R.ok(sysI18nService.getSupportedLanguages());
    }
}
