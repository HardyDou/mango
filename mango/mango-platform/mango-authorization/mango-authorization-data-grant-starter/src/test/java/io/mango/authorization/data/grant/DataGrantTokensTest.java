package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Grant 请求头多值解析测试。
 *
 * <p>覆盖逗号切分、去重、控制字符与超长取值丢弃、以及取值个数 / 总长度超限时整体按「未携带」处理。</p>
 */
class DataGrantTokensTest {

    @Test
    void splitsCommaSeparatedValuesAndTrims() {
        assertThat(DataGrantTokens.parse(List.of("g1, g2 ,, g3"))).containsExactly("g1", "g2", "g3");
    }

    @Test
    void splitsAcrossHeaderLines() {
        assertThat(DataGrantTokens.parse(List.of("g1", "g2,g3"))).containsExactly("g1", "g2", "g3");
    }

    @Test
    void deduplicatesPreservingOrder() {
        assertThat(DataGrantTokens.parse(List.of("g2,g1,g2"))).containsExactly("g2", "g1");
    }

    @Test
    void dropsValuesWithWhitespaceOrControlCharacters() {
        assertThat(DataGrantTokens.parse(List.of("g1,\"bad token\",bad\u0007value,g2")))
                .containsExactly("g1", "g2");
    }

    @Test
    void dropsValuesWithNonAsciiCharacters() {
        assertThat(DataGrantTokens.parse(List.of("g1,令牌"))).containsExactly("g1");
    }

    @Test
    void dropsOversizedSingleValue() {
        String huge = "a".repeat(DataGrantHeaders.MAX_TOKEN_LENGTH + 1);

        assertThat(DataGrantTokens.parse(List.of(huge + ",g1"))).containsExactly("g1");
    }

    @Test
    void treatsTooManyValuesAsAbsent() {
        StringBuilder header = new StringBuilder();
        for (int index = 0; index <= DataGrantHeaders.MAX_TOKENS; index++) {
            if (index > 0) {
                header.append(',');
            }
            header.append("token-").append(index);
        }

        assertThat(DataGrantTokens.parse(List.of(header.toString()))).isEmpty();
    }

    @Test
    void treatsOverlongHeaderAsAbsent() {
        String header = "a".repeat(DataGrantHeaders.MAX_HEADER_LENGTH + 1);

        assertThat(DataGrantTokens.parse(List.of(header))).isEmpty();
    }

    @Test
    void toleratesEmptyAndNullInput() {
        assertThat(DataGrantTokens.parse(null)).isEmpty();
        assertThat(DataGrantTokens.parse(List.of())).isEmpty();
        assertThat(DataGrantTokens.parse(List.of("  ", ","))).isEmpty();
    }

    @Test
    void keepsExactlyMaxTokens() {
        StringBuilder header = new StringBuilder();
        for (int index = 0; index < DataGrantHeaders.MAX_TOKENS; index++) {
            if (index > 0) {
                header.append(',');
            }
            header.append("token-").append(index);
        }

        assertThat(DataGrantTokens.parse(List.of(header.toString()))).hasSize(DataGrantHeaders.MAX_TOKENS);
    }
}
