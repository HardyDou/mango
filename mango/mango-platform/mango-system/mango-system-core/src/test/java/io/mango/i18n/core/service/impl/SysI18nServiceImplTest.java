package io.mango.i18n.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import io.mango.i18n.core.entity.SysI18nEntity;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.infra.context.support.TtlExecutorDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysI18nServiceImplTest {

    @Mock
    private SysI18nMapper mapper;

    private SysI18nService service;

    @BeforeEach
    void setUp() {
        service = new SysI18nService(mapper, new TtlExecutorDecorator());
    }

    @Test
    void listMapKeepsOriginalDynamicJsonShape() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(message("hello", "你好", "Hello")));

        I18nLanguagePackVO result = service.listMap();

        assertThat(result.asJson()).containsOnlyKeys("zh-cn", "en");
        assertThat(result.asJson().get("zh-cn").getFirst().asJson()).containsEntry("hello", "你好");
        assertThat(result.asJson().get("en").getFirst().asJson()).containsEntry("hello", "Hello");
    }

    @Test
    void listMapReturnsEmptyListsWhenNoData() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(service.listMap().asJson().get("zh-cn")).isEmpty();
        assertThat(service.listMap().asJson().get("en")).isEmpty();
    }

    @Test
    void emptyStartupCacheReloadsAfterResourceInitialization() {
        when(mapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(), List.of(message("common.save", "保存", "Save")));

        assertThat(service.listByLang("zh-cn")).isEmpty();
        assertThat(service.listByLang("zh-cn").getFirst().asJson()).containsEntry("common.save", "保存");
    }

    @Test
    void listByLangIsCaseInsensitiveAndFallsBackToKey() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(message("welcome", null, "Welcome")));

        List<I18nEntryVO> chinese = service.listByLang("ZH-CN");
        List<I18nEntryVO> english = service.listByLang("fr");

        assertThat(chinese.getFirst().asJson()).containsEntry("welcome", "welcome");
        assertThat(english.getFirst().asJson()).containsEntry("welcome", "Welcome");
    }

    @Test
    void languagePackPreservesRequestedLanguageAsJsonKey() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(message("welcome", "欢迎", "Welcome")));

        assertThat(service.languagePack("zh_CN").asJson())
                .containsOnlyKeys("zh_CN");
        assertThat(service.languagePack("ja").asJson())
                .containsOnlyKeys("ja");
    }

    @Test
    void supportedLanguagesRemainCompatible() {
        assertThat(service.getSupportedLanguages()).containsExactly("zh-cn", "en");
    }

    private SysI18nEntity message(String name, String zhCn, String en) {
        SysI18nEntity entity = new SysI18nEntity();
        entity.setName(name);
        entity.setZhCn(zhCn);
        entity.setEn(en);
        return entity;
    }
}
