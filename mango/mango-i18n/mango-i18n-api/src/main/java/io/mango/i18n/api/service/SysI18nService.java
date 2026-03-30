package io.mango.i18n.api.service;

import io.mango.i18n.api.entity.SysI18n;

import java.util.List;
import java.util.Map;

/**
 * i18n service interface
 *
 * @author Mango
 */
public interface SysI18nService {

    /**
     * Get all i18n entries as a map grouped by language
     *
     * @return map of language -> list of key-value pairs
     */
    Map<String, List<Map<String, String>>> listMap();

    /**
     * Get i18n entries for a specific language
     *
     * @param lang language code (e.g., "zh-cn", "en")
     * @return list of key-value pairs
     */
    List<Map<String, String>> listByLang(String lang);

    /**
     * Get all supported languages
     *
     * @return list of language codes
     */
    List<String> getSupportedLanguages();

    /**
     * Get i18n entry by key
     *
     * @param name i18n key
     * @return sys i18n entity
     */
    SysI18n getByName(String name);
}
