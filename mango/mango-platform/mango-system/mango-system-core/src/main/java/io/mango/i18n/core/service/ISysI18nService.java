package io.mango.i18n.core.service;

import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import io.mango.i18n.api.vo.SysI18nMessageVO;

import java.util.List;

public interface ISysI18nService {
    I18nLanguagePackVO listMap();
    List<I18nEntryVO> listByLang(String lang);
    I18nLanguagePackVO languagePack(String lang);
    List<String> getSupportedLanguages();
    SysI18nMessageVO getByName(String name);
}
