package io.mango.infra.docsign.command;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignFormat;

import java.util.Arrays;

/**
 * Document signature validation command. Document content is optional for the streaming API.
 */
@LocalCapabilityContract
public final class DocumentVerifyCommand {

    private final DocumentSignFormat format;
    private final byte[] content;
    private final TrustStoreMaterial trustStore;
    private final char[] documentPassword;

    private DocumentVerifyCommand(Builder builder) {
        Require.notNull(builder.format, "文档格式不能为空");
        this.format = builder.format;
        this.content = builder.content == null ? null : Arrays.copyOf(builder.content, builder.content.length);
        this.trustStore = builder.trustStore;
        this.documentPassword = builder.documentPassword == null
                ? new char[0] : Arrays.copyOf(builder.documentPassword, builder.documentPassword.length);
    }

    public DocumentSignFormat format() {
        return format;
    }

    public byte[] content() {
        return content == null ? null : Arrays.copyOf(content, content.length);
    }

    public boolean hasContent() {
        return content != null && content.length > 0;
    }

    public TrustStoreMaterial trustStore() {
        return trustStore;
    }

    public boolean hasTrustStore() {
        return trustStore != null;
    }

    public char[] documentPassword() {
        return Arrays.copyOf(documentPassword, documentPassword.length);
    }

    public static Builder builder() {
        return new Builder();
    }

    @LocalCapabilityContract
    public static final class Builder {

        private DocumentSignFormat format;
        private byte[] content;
        private TrustStoreMaterial trustStore;
        private char[] documentPassword;

        private Builder() {
        }

        public Builder format(DocumentSignFormat format) {
            this.format = format;
            return this;
        }

        public Builder content(byte[] content) {
            this.content = content == null ? null : Arrays.copyOf(content, content.length);
            return this;
        }

        public Builder trustStore(TrustStoreMaterial trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public Builder documentPassword(char[] documentPassword) {
            this.documentPassword = documentPassword == null
                    ? null : Arrays.copyOf(documentPassword, documentPassword.length);
            return this;
        }

        public DocumentVerifyCommand build() {
            return new DocumentVerifyCommand(this);
        }
    }
}
