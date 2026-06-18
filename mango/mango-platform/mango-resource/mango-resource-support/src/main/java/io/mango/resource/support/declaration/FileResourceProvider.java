package io.mango.resource.support.declaration;

import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
}
