package io.mango.infra.sensitive.core;

import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SensitiveMaskerStrategyTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategyCases")
    void mask_withEveryBuiltInStrategy_preservesEstablishedFormat(
            String fieldName, String rawValue, String expectedValue) throws Exception {
        Sensitive sensitive = annotation(fieldName);

        assertThat(SensitiveMasker.mask(sensitive, rawValue)).isEqualTo(expectedValue);
    }

    @Test
    void strategyFixture_coversEveryEnumValueExactlyOnce() {
        Set<SensitiveType> coveredTypes = Arrays.stream(StrategyFixture.class.getDeclaredFields())
                .map(field -> field.getAnnotation(Sensitive.class))
                .map(Sensitive::type)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(coveredTypes).containsExactlyInAnyOrder(SensitiveType.values());
    }

    @ParameterizedTest
    @MethodSource("shortValueCases")
    void builtInStrategies_withShortOrMalformedValues_neverReturnPlaintext(
            String fieldName, String rawValue) throws Exception {
        Sensitive sensitive = annotation(fieldName);

        assertThatCode(() -> SensitiveMasker.mask(sensitive, rawValue)).doesNotThrowAnyException();
        assertThat(SensitiveMasker.mask(sensitive, rawValue)).isNotEqualTo(rawValue);
    }

    @Test
    void custom_withUnicodeAndInvalidVisibilityLengths_masksByCodePointWithoutLeaking() {
        assertThat(SensitiveMasker.custom("😀密码", -1, 99, "#")).isEqualTo("#密码");
        assertThat(SensitiveMasker.custom("秘密", 99, 0, "*")).isEqualTo("秘*");
        assertThat(SensitiveMasker.custom("", 99, 99, "*")).isEmpty();
        assertThat(SensitiveMasker.custom(null, 1, 1, "*")).isNull();
    }

    @Test
    void json_withNestedArraysNullAndNonStringValues_masksRecursively() throws Exception {
        Sensitive sensitive = annotation("json");
        String raw = "{\"items\":[{\"token\":\"abcdef\"},{\"token\":123}],\"token\":null}";

        String masked = SensitiveMasker.mask(sensitive, raw);

        assertThat(masked)
                .contains("\"token\":\"***def\"", "\"token\":\"******\"", "\"token\":null")
                .doesNotContain("abcdef", ":123");
    }

    @Test
    void json_withMalformedShortInput_neverReturnsPlaintextOrThrows() throws Exception {
        Sensitive sensitive = annotation("json");

        assertThat(SensitiveMasker.mask(sensitive, "abc")).isEqualTo("******");
    }

    private static Stream<Arguments> strategyCases() {
        return Stream.of(
                Arguments.of("custom", "abcdef", "a####f"),
                Arguments.of("customer", "abcdef", "a****f"),
                Arguments.of("chineseName", "张三", "*三"),
                Arguments.of("idCard", "110110199901011234", "110110********1234"),
                Arguments.of("fixedPhone", "01012345678", "*******5678"),
                Arguments.of("mobilePhone", "17612345678", "176****5678"),
                Arguments.of("address", "北京市海淀区中关村", "北京市海淀区***"),
                Arguments.of("email", "service@example.com", "s******@example.com"),
                Arguments.of("bankCard", "6222021234567890", "622202******7890"),
                Arguments.of("password", "secret", "******"),
                Arguments.of("key", "abcdef123456", "***456"),
                Arguments.of("ipv4", "113.123.198.176", "113.123.198.*"),
                Arguments.of("carLicense", "粤B12345", "粤B1***5"),
                Arguments.of("queryParam", "https://example.test?a=1&b=2#top",
                        "https://example.test?a=***&b=***#top"),
                Arguments.of("json", "{\"token\":\"abcdef\",\"name\":\"mango\"}",
                        "{\"token\":\"***def\",\"name\":\"mango\"}"));
    }

    private static Stream<Arguments> shortValueCases() {
        return Stream.of(
                Arguments.of("custom", "a"),
                Arguments.of("customer", "a"),
                Arguments.of("chineseName", "张"),
                Arguments.of("idCard", "1234567890"),
                Arguments.of("fixedPhone", "1234"),
                Arguments.of("mobilePhone", "1234567"),
                Arguments.of("address", "北京市海淀"),
                Arguments.of("email", "a@example.com"),
                Arguments.of("email", "@example.com"),
                Arguments.of("bankCard", "1234567890"),
                Arguments.of("password", "x"),
                Arguments.of("key", "abc"),
                Arguments.of("ipv4", "not-ip"),
                Arguments.of("carLicense", "粤"),
                Arguments.of("json", "abc"));
    }

    private static Sensitive annotation(String fieldName) throws NoSuchFieldException {
        Field field = StrategyFixture.class.getDeclaredField(fieldName);
        return field.getAnnotation(Sensitive.class);
    }

    @SuppressWarnings("unused")
    private static class StrategyFixture {

        @Sensitive(type = SensitiveType.CUSTOM, prefixNoMaskLen = 1, suffixNoMaskLen = 1, maskStr = "#")
        String custom;

        @Sensitive(type = SensitiveType.CUSTOMER, prefixNoMaskLen = 1, suffixNoMaskLen = 1)
        String customer;

        @Sensitive(type = SensitiveType.CHINESE_NAME)
        String chineseName;

        @Sensitive(type = SensitiveType.ID_CARD)
        String idCard;

        @Sensitive(type = SensitiveType.FIXED_PHONE)
        String fixedPhone;

        @Sensitive(type = SensitiveType.MOBILE_PHONE)
        String mobilePhone;

        @Sensitive(type = SensitiveType.ADDRESS)
        String address;

        @Sensitive(type = SensitiveType.EMAIL)
        String email;

        @Sensitive(type = SensitiveType.BANK_CARD)
        String bankCard;

        @Sensitive(type = SensitiveType.PASSWORD)
        String password;

        @Sensitive(type = SensitiveType.KEY)
        String key;

        @Sensitive(type = SensitiveType.IPV4)
        String ipv4;

        @Sensitive(type = SensitiveType.CAR_LICENSE)
        String carLicense;

        @Sensitive(type = SensitiveType.QUERY_PARAM)
        String queryParam;

        @Sensitive(type = SensitiveType.JSON, keys = "token")
        String json;
    }
}
