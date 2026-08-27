package io.mango.resource.support.declaration;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 从 classpath JSON/YAML 声明文件提供资源。
 */
@RequiredArgsConstructor
public class FileResourceProvider implements ResourceProvider {

    private final ResourceDeclarationLoader loader;

    @Override
    public List<ResourceDeclaration> provide() {
        return loader.load();
    }

    @Override
    public Map<String, List<String>> moduleDependencies() {
        return loader.loadModuleDependencies();
    }
}
