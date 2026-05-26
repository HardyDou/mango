package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.SensitiveMaskingContext;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;
import io.mango.infra.sensitive.core.SensitiveMaskingRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveJacksonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SensitiveJacksonModule());

    @AfterEach
    void tearDown() {
        SensitiveMaskingRuntime.reset();
    }

    @Test
    void serialize_withSensitiveAnnotation_masksOutputButKeepsObjectValue() throws Exception {
        AccountView view = new AccountView("17612345678", "plain");

        String json = objectMapper.writeValueAsString(view);

        assertThat(json).contains("\"mobile\":\"176****5678\"");
        assertThat(view.mobile).isEqualTo("17612345678");
    }

    @Test
    void serialize_whenContextDisabled_outputsRawValue() throws Exception {
        AccountView view = new AccountView("17612345678", "plain");

        String json = SensitiveMaskingContext.getWithoutMasking(() -> write(view));

        assertThat(json).contains("\"mobile\":\"17612345678\"");
    }

    @Test
    void serialize_whenMaskingServiceAllowsRaw_outputsRawValue() throws Exception {
        ISensitiveMaskingService service = sensitive -> false;
        SensitiveMaskingRuntime.setMaskingService(service);

        String json = objectMapper.writeValueAsString(new AccountView("17612345678", "plain"));

        assertThat(json).contains("\"mobile\":\"17612345678\"");
    }

    @Test
    void serialize_withJsonSensitiveType_masksNestedConfiguredKeys() throws Exception {
        ConfigView view = new ConfigView("""
                {"channel":"wechat","credential":{"appSecret":"abcdef123456","token":"token-value"},"publicName":"mango"}
                """);

        String json = objectMapper.writeValueAsString(view);

        assertThat(json).contains("\\\"appSecret\\\":\\\"***456\\\"");
        assertThat(json).contains("\\\"token\\\":\\\"***lue\\\"");
        assertThat(json).contains("\\\"publicName\\\":\\\"mango\\\"");
    }

    @Test
    void serialize_withJsonFuzzyKey_masksMatchedKeysIgnoringCase() throws Exception {
        FuzzyConfigView view = new FuzzyConfigView("""
                {"smsSecretKey":"abcdef123456","name":"aliyun"}
                """);

        String json = objectMapper.writeValueAsString(view);

        assertThat(json).contains("\\\"smsSecretKey\\\":\\\"***456\\\"");
        assertThat(json).contains("\\\"name\\\":\\\"aliyun\\\"");
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static class AccountView {

        @Sensitive(type = SensitiveType.MOBILE_PHONE)
        private final String mobile;

        private final String name;

        AccountView(String mobile, String name) {
            this.mobile = mobile;
            this.name = name;
        }

        public String getMobile() {
            return mobile;
        }

        public String getName() {
            return name;
        }
    }

    static class ConfigView {

        @Sensitive(type = SensitiveType.JSON, keys = {"appSecret", "token"})
        private final String configJson;

        ConfigView(String configJson) {
            this.configJson = configJson;
        }

        public String getConfigJson() {
            return configJson;
        }
    }

    static class FuzzyConfigView {

        @Sensitive(type = SensitiveType.JSON, keys = {"secret"}, fuzzy = true)
        private final String configJson;

        FuzzyConfigView(String configJson) {
            this.configJson = configJson;
        }

        public String getConfigJson() {
            return configJson;
        }
    }
}
