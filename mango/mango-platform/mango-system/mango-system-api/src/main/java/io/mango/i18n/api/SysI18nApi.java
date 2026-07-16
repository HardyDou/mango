package io.mango.i18n.api;

import io.mango.common.result.R;
import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import io.mango.i18n.api.vo.SysI18nMessageVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface SysI18nApi {

    R<I18nLanguagePackVO> publicInfo();

    R<List<I18nEntryVO>> publicInfoByLang(@NotBlank @Size(max = 16) String lang);

    R<List<String>> languages();

    R<SysI18nMessageVO> getByName(@NotBlank @Size(max = 100) String name);

    R<I18nLanguagePackVO> i18n(@NotBlank @Size(max = 16) String lang);
}
