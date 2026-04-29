package io.mango.infra.web.support;

import io.mango.infra.web.api.IInternalPathProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 基于 @Inner 请求映射的内部路径提供器。
 */
public class InnerMappingInternalPathProvider implements IInternalPathProvider {

    private final Set<String> paths = new CopyOnWriteArraySet<>();

    @Override
    public List<String> getInternalPaths() {
        return List.copyOf(paths);
    }

    public void replacePaths(Set<String> discoveredPaths) {
        paths.clear();
        if (discoveredPaths != null) {
            paths.addAll(discoveredPaths);
        }
    }
}
