package io.mango.i18n.api.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Schema(description = "国际化语言包")
public class I18nLanguagePackVO {

    @JsonIgnore
    @Schema(description = "简体中文国际化条目")
    private final List<I18nEntryVO> zhCn;

    @JsonIgnore
    @Schema(description = "英文国际化条目")
    private final List<I18nEntryVO> en;

    @JsonIgnore
    @Schema(description = "单语言包的语言编码")
    private final String language;

    @JsonIgnore
    @Schema(description = "单语言包的国际化条目")
    private final List<I18nEntryVO> entries;

    private I18nLanguagePackVO(List<I18nEntryVO> zhCn, List<I18nEntryVO> en,
                               String language, List<I18nEntryVO> entries) {
        this.zhCn = immutableCopy(zhCn);
        this.en = immutableCopy(en);
        this.language = language;
        this.entries = immutableCopy(entries);
    }

    public static I18nLanguagePackVO all(List<I18nEntryVO> zhCn, List<I18nEntryVO> en) {
        return new I18nLanguagePackVO(zhCn, en, null, null);
    }

    public static I18nLanguagePackVO single(String language, List<I18nEntryVO> entries) {
        return new I18nLanguagePackVO(null, null, language, entries);
    }

    public List<I18nEntryVO> getZhCn() {
        return immutableCopy(zhCn);
    }

    public List<I18nEntryVO> getEn() {
        return immutableCopy(en);
    }

    public List<I18nEntryVO> getEntries() {
        return immutableCopy(entries);
    }

    @JsonAnyGetter
    public Map<String, List<I18nEntryVO>> asJson() {
        if (language != null) {
            return Map.of(language, entries);
        }
        Map<String, List<I18nEntryVO>> languages = new LinkedHashMap<>();
        languages.put("zh-cn", zhCn);
        languages.put("en", en);
        return languages;
    }

    private static List<I18nEntryVO> immutableCopy(List<I18nEntryVO> values) {
        if (values == null) {
            return null;
        }
        return List.copyOf(values);
    }
}
