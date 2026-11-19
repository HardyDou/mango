package io.mango.plugin.check;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CheckstyleFingerprintTokensTest {

    @Test
    void stripsConfiguredThresholdForEveryLocaleAndParenthesisStyle() {
        String english = CheckstyleFingerprintTokens.of("Cyclomatic complexity is 24 (max allowed is 20).");
        String englishOldThreshold =
                CheckstyleFingerprintTokens.of("Cyclomatic complexity is 24 (max allowed is 15).");
        String chinese = CheckstyleFingerprintTokens.of("圈复杂度： 24 （最多： 20 ）。");
        String chineseOldThreshold = CheckstyleFingerprintTokens.of("圈复杂度： 24 （最多： 15 ）。");

        assertEquals("number:24", english);
        assertEquals("number:24", englishOldThreshold);
        assertEquals("number:24", chinese);
        assertEquals("number:24", chineseOldThreshold);
    }

    @Test
    void keepsDifferentActualValuesDistinct() {
        String shallow = CheckstyleFingerprintTokens.of("Cyclomatic complexity is 24 (max allowed is 20).");
        String deep = CheckstyleFingerprintTokens.of("Cyclomatic complexity is 30 (max allowed is 20).");

        assertNotEquals(shallow, deep);
        assertEquals("number:30", deep);
    }

    @Test
    void keepsDescriptionsWithoutConfiguredThresholdUnchanged() {
        assertEquals("number:24,number:28",
                CheckstyleFingerprintTokens.of("Indentation is not correct (expected level is 28, but is 24)."));
        assertEquals("literal:030", CheckstyleFingerprintTokens.of("Avoid using a magic number ('030')."));
    }
}
