package io.mango.infra.fileproc.render.service;

import io.mango.infra.fileproc.render.enums.RenderFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 本地文档渲染器注册表。
 */
public class RenderRegistry {

    private final List<IRenderProvider> providers = new ArrayList<>();

    public RenderRegistry(Collection<IRenderProvider> providers) {
        if (providers != null) {
            this.providers.addAll(providers);
        }
    }

    public Optional<IRenderProvider> findProvider(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return providers.stream()
                .filter(provider -> provider.supports(sourceFormat, targetFormat))
                .findFirst();
    }

    public List<IRenderProvider> providers() {
        return List.copyOf(providers);
    }
}
