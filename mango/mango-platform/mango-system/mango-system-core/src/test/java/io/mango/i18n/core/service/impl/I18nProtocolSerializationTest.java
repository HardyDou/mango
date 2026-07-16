package io.mango.i18n.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.i18n.api.vo.I18nEntryVO;
import io.mango.i18n.api.vo.I18nLanguagePackVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class I18nProtocolSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void entrySerializesAsOriginalDynamicKeyValueObject() throws Exception {
        assertThat(objectMapper.writeValueAsString(new I18nEntryVO("hello", "你好")))
                .isEqualTo("{\"hello\":\"你好\"}");
    }

    @Test
    void completePackKeepsOriginalZhCnAndEnShape() throws Exception {
        I18nLanguagePackVO pack = I18nLanguagePackVO.all(
                List.of(new I18nEntryVO("hello", "你好")),
                List.of(new I18nEntryVO("hello", "Hello")));

        assertThat(objectMapper.writeValueAsString(pack))
                .isEqualTo("{\"zh-cn\":[{\"hello\":\"你好\"}],\"en\":[{\"hello\":\"Hello\"}]}");
    }

    @Test
    void singlePackPreservesRequestedLanguageKey() throws Exception {
        I18nLanguagePackVO pack = I18nLanguagePackVO.single(
                "zh_CN", List.of(new I18nEntryVO("hello", "你好")));

        assertThat(objectMapper.writeValueAsString(pack))
                .isEqualTo("{\"zh_CN\":[{\"hello\":\"你好\"}]}");
    }
}
