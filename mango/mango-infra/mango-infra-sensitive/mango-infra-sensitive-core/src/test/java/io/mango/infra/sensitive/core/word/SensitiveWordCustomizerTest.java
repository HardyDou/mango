package io.mango.infra.sensitive.core.word;

import io.mango.infra.sensitive.api.ISensitiveWordProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveWordCustomizerTest {

    @Test
    void allowAndDeny_withMultipleProviders_mergesWordsInOrder() {
        SensitiveWordCustomizer customizer = new SensitiveWordCustomizer(List.of(
                provider(List.of("mango"), List.of("blocked-a")),
                provider(List.of("safe"), List.of("blocked-b"))));

        assertThat(customizer.allow()).containsExactly("mango", "safe");
        assertThat(customizer.deny()).containsExactly("blocked-a", "blocked-b");
    }

    @Test
    void allowAndDeny_withNullProviderResults_treatsThemAsEmptyAndDropsNullWords() {
        SensitiveWordCustomizer customizer = new SensitiveWordCustomizer(List.of(
                provider(null, null),
                provider(java.util.Arrays.asList("safe", null), java.util.Arrays.asList(null, "blocked"))));

        assertThat(customizer.allow()).containsExactly("safe");
        assertThat(customizer.deny()).containsExactly("blocked");
    }

    private ISensitiveWordProvider provider(List<String> allowWords, List<String> denyWords) {
        return new ISensitiveWordProvider() {
            @Override
            public List<String> allowWords() {
                return allowWords;
            }

            @Override
            public List<String> denyWords() {
                return denyWords;
            }
        };
    }
}
