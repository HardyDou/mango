package io.mango.infra.sensitive.core.word;

import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import io.mango.infra.sensitive.api.ISensitiveWordProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Bridges Mango word providers to the houbb sensitive-word engine.
 */
public class SensitiveWordCustomizer implements IWordAllow, IWordDeny {

    private final List<ISensitiveWordProvider> providers;

    public SensitiveWordCustomizer(Collection<ISensitiveWordProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
    }

    @Override
    public List<String> allow() {
        List<String> words = new ArrayList<>();
        for (ISensitiveWordProvider provider : providers) {
            addWords(words, provider.allowWords());
        }
        return words;
    }

    @Override
    public List<String> deny() {
        List<String> words = new ArrayList<>();
        for (ISensitiveWordProvider provider : providers) {
            addWords(words, provider.denyWords());
        }
        return words;
    }

    private void addWords(List<String> destination, Collection<String> source) {
        if (source != null) {
            source.stream().filter(Objects::nonNull).forEach(destination::add);
        }
    }
}
