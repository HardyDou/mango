package io.mango.infra.docsign.command;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;

import java.util.Arrays;

/**
 * Caller-supplied certificate trust store used for fail-closed validation.
 */
@LocalCapabilityContract
public final class TrustStoreMaterial {

    private final String type;

    private final byte[] content;

    private final char[] password;

    public TrustStoreMaterial(String type, byte[] content, char[] password) {
        Require.isTrue(content != null && content.length > 0, "信任库内容不能为空");
        this.type = type == null || type.isBlank() ? "PKCS12" : type;
        this.content = Arrays.copyOf(content, content.length);
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
    }

    public String type() {
        return type;
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public char[] password() {
        return Arrays.copyOf(password, password.length);
    }
}
