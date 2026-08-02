package io.mango.resource.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.support.declaration.ResourceDeclarationCanonicalizer;
import io.mango.resource.support.model.ResourceDeclaration;
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
}
