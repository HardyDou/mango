package io.mango.infra.docsign.command;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Arrays;

/**
 * PKCS#12 signing key material supplied by the caller.
 * <p>
 * The module never reads signing keys from repository configuration and never logs passwords or key bytes.
 */
@LocalCapabilityContract
public final class Pkcs12KeyMaterial {

    private final byte[] content;

    private final char[] password;

    private final String alias;

    public Pkcs12KeyMaterial(byte[] content, char[] password, String alias) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("PKCS#12 内容不能为空");
        }
        this.content = Arrays.copyOf(content, content.length);
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        this.alias = alias == null || alias.isBlank() ? null : alias;
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public char[] password() {
        return Arrays.copyOf(password, password.length);
    }

    public String alias() {
        return alias;
    }
}
