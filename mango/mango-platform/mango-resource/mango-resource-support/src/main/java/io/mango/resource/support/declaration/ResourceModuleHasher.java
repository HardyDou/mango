package io.mango.resource.support.declaration;

import io.mango.resource.support.model.ResourceDeclaration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Computes the stable SHA-256 identity of one complete Resource module. */
public final class ResourceModuleHasher {

    private final ResourceDeclarationCanonicalizer canonicalizer;

    public ResourceModuleHasher(ResourceDeclarationCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    public String hash(String moduleCode, List<String> dependencies, List<ResourceDeclaration> declarations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "resource-module-v1\n");
            update(digest, moduleCode == null ? "" : moduleCode.trim());
            update(digest, "\n");
            dependencies.stream().map(String::trim).distinct().sorted()
                    .forEach(dependency -> update(digest, "dependency=" + dependency + "\n"));
            declarations.stream().sorted(Comparator.comparing(ResourceDeclaration::getId))
                    .forEach(declaration -> {
                        digest.update(canonicalizer.canonicalBytes(declaration));
                        digest.update((byte) '\n');
                    });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }
}
