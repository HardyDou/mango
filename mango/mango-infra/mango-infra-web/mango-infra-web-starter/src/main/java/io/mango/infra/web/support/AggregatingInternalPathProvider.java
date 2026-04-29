package io.mango.infra.web.support;

import io.mango.infra.web.api.IInternalPathProvider;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 聚合多个提供器暴露的内部路径。
 */
public class AggregatingInternalPathProvider implements IInternalPathProvider {

    private final ObjectProvider<IInternalPathProvider> providers;

    public AggregatingInternalPathProvider(ObjectProvider<IInternalPathProvider> providers) {
        this.providers = providers;
    }

    @Override
    public List<String> getInternalPaths() {
        Set<String> paths = new LinkedHashSet<>();
        for (IInternalPathProvider provider : providers) {
            if (provider == this) {
                continue;
            }
            List<String> providedPaths = provider.getInternalPaths();
            if (providedPaths != null) {
                paths.addAll(providedPaths.stream()
                        .filter(path -> path != null && !path.isBlank())
                        .toList());
            }
        }
        return List.copyOf(paths);
    }
}
