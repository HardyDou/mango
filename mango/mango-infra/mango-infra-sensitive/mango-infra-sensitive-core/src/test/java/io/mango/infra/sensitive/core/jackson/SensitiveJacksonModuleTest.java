package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.SensitiveMaskingContext;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveJacksonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SensitiveJacksonModule());

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
        ObjectMapper rawObjectMapper = new ObjectMapper()
                .registerModule(new SensitiveJacksonModule(service));

        String json = rawObjectMapper.writeValueAsString(new AccountView("17612345678", "plain"));

        assertThat(json).contains("\"mobile\":\"17612345678\"");
    }

    @Test
    void serialize_withIndependentModules_keepsMaskingPoliciesIsolated() throws Exception {
        ObjectMapper rawObjectMapper = new ObjectMapper()
                .registerModule(new SensitiveJacksonModule(sensitive -> false));
        ObjectMapper maskingObjectMapper = new ObjectMapper()
                .registerModule(new SensitiveJacksonModule(sensitive -> true));
        AccountView view = new AccountView("17612345678", "plain");

        assertThat(rawObjectMapper.writeValueAsString(view))
                .contains("\"mobile\":\"17612345678\"");
        assertThat(maskingObjectMapper.writeValueAsString(view))
                .contains("\"mobile\":\"176****5678\"");
        assertThat(rawObjectMapper.writeValueAsString(view))
                .contains("\"mobile\":\"17612345678\"");
    }

    @Test
    void serialize_withGetterAnnotation_masksOutput() throws Exception {
        String json = objectMapper.writeValueAsString(new GetterAnnotatedView("17612345678"));

        assertThat(json).contains("\"mobile\":\"176****5678\"");
    }

    @Test
    void serialize_withNestedCollections_masksEveryAnnotatedValue() throws Exception {
        NestedView view = new NestedView(
                List.of(new AccountView("17612345678", "first")),
                Map.of("owner", new AccountView("13912345678", "second")));

        String json = objectMapper.writeValueAsString(view);

        assertThat(json).contains("176****5678", "139****5678");
        assertThat(json).doesNotContain("17612345678", "13912345678");
    }

    @Test
    void serialize_withNullAndBlankValues_preservesNonSecretEmptySemantics() throws Exception {
        assertThat(objectMapper.writeValueAsString(new AccountView(null, "plain")))
                .contains("\"mobile\":null");
        assertThat(objectMapper.writeValueAsString(new AccountView(" ", "plain")))
                .contains("\"mobile\":\" \"");
    }

    @Test
    void deserialize_thenSerialize_preservesRawObjectAndMasksOnlyOutput() throws Exception {
        MutableAccountView view = objectMapper.readValue(
                "{\"mobile\":\"17612345678\",\"name\":\"plain\"}", MutableAccountView.class);

        assertThat(view.mobile).isEqualTo("17612345678");
        assertThat(objectMapper.writeValueAsString(view))
                .contains("\"mobile\":\"176****5678\"")
                .doesNotContain("17612345678");
    }

    @Test
    void serialize_whenPolicyFails_doesNotIncludeRawValueInExceptionChain() {
        String rawValue = "17612345678";
        ObjectMapper failingObjectMapper = new ObjectMapper()
                .registerModule(new SensitiveJacksonModule(sensitive -> {
                    throw new IllegalStateException("masking policy unavailable");
                }));

        try {
            failingObjectMapper.writeValueAsString(new AccountView(rawValue, "plain"));
        } catch (Exception exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                assertThat(cause.getMessage()).doesNotContain(rawValue);
            }
            return;
        }
        throw new AssertionError("expected serialization to fail");
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

    static class GetterAnnotatedView {

        private final String mobile;

        GetterAnnotatedView(String mobile) {
            this.mobile = mobile;
        }

        @Sensitive(type = SensitiveType.MOBILE_PHONE)
        public String getMobile() {
            return mobile;
        }
    }

    record NestedView(List<AccountView> accounts, Map<String, AccountView> accountByRole) {
    }

    static class MutableAccountView {

        @Sensitive(type = SensitiveType.MOBILE_PHONE)
        public String mobile;

        public String name;
    }
}
