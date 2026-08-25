package io.mango.resource.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.declaration.ResourceModuleHasher;
import org.springframework.util.DigestUtils;

/**
 * 计算资源声明内容 hash。
 */
public class ResourceContentHasher {

    private final ResourceDeclarationCanonicalizer canonicalizer;

    public ResourceContentHasher(ObjectMapper objectMapper) {
        this.canonicalizer = new ResourceDeclarationCanonicalizer(objectMapper);
    }

    public String hash(ResourceDeclaration declaration) {
        return DigestUtils.md5DigestAsHex(canonicalizer.canonicalBytes(declaration));
    }

    public String moduleHash(String moduleCode, java.util.List<String> dependencies,
                             java.util.List<ResourceDeclaration> declarations) {
        return new ResourceModuleHasher(canonicalizer).hash(moduleCode, dependencies, declarations);
    }
}
