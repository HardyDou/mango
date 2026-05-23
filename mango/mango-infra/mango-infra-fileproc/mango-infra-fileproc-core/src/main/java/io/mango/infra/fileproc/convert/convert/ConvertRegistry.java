package io.mango.infra.fileproc.convert.convert;

import io.mango.infra.fileproc.convert.enums.ConvertFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 本地转换器注册表。
 */
public class ConvertRegistry {

    private final List<IConvertProvider> providers = new ArrayList<>();

    public ConvertRegistry(Collection<IConvertProvider> providers) {
        if (providers != null) {
            this.providers.addAll(providers);
        }
    }

    public Optional<IConvertProvider> findProvider(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return providers.stream()
                .filter(converter -> converter.supports(sourceFormat, targetFormat))
                .findFirst();
    }

    public List<IConvertProvider> providers() {
        return List.copyOf(providers);
    }
}
