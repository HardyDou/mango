package io.mango.i18n.starter.remote;

import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * I18n Feign client for remote i18n operations
 *
 * @author Mango
 */
@FeignClient(name = "i18n-service", path = "/i18n")
public interface I18nFeignClient {

    /**
     * Get all i18n entries grouped by language
     *
     * @return map of language -> list of i18n entries
     */
    @GetMapping("/public")
    R<Map<String, List<Map<String, String>>>> listMap();

    /**
     * Get i18n entries for a specific language
     *
     * @param lang language code (e.g., "zh_CN", "en_US")
     * @return list of i18n entries for the language
     */
    @GetMapping("/public/lang")
    R<List<Map<String, String>>> listByLang(@RequestParam String lang);

    /**
     * Get supported languages
     *
     * @return list of supported language codes
     */
    @GetMapping("/languages")
    R<List<String>> getSupportedLanguages();
}
