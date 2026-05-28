package io.mango.infra.sensitive.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveMaskerTest {

    @Test
    void mobilePhone_withStandardNumber_masksMiddleDigits() {
        assertThat(SensitiveMasker.mobilePhone("17612345678")).isEqualTo("176****5678");
    }

    @Test
    void idCard_withEighteenDigits_keepsPrefixAndSuffix() {
        assertThat(SensitiveMasker.idCard("110110199901011234")).isEqualTo("110110********1234");
    }

    @Test
    void email_withNormalAddress_masksPrefixOnly() {
        assertThat(SensitiveMasker.email("service@example.com")).isEqualTo("s******@example.com");
    }

    @Test
    void key_withLongSecret_returnsSixVisibleMaskShape() {
        assertThat(SensitiveMasker.key("abcdef123456")).isEqualTo("***456");
    }

    @Test
    void ipv4_withAddress_masksLastSegment() {
        assertThat(SensitiveMasker.ipv4("113.123.198.176")).isEqualTo("113.123.198.*");
    }

    @Test
    void queryParam_withUrl_masksEveryParameterValue() {
        assertThat(SensitiveMasker.queryParam("https://example.test/callback?token=abc&appSecret=def#top"))
                .isEqualTo("https://example.test/callback?token=***&appSecret=***#top");
    }
}
