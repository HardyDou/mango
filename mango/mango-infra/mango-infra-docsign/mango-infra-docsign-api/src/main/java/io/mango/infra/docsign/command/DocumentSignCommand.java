package io.mango.infra.docsign.command;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.enums.DocumentSignatureAlgorithm;

import java.util.Arrays;

/**
 * Document signing command. Document content is optional for the streaming API. It contains no
 * file centre, tenant or permission information.
 */
@LocalCapabilityContract
public final class DocumentSignCommand {

    private final DocumentSignFormat format;
    private final byte[] content;
    private final Pkcs12KeyMaterial keyMaterial;
    private final DocumentSignatureAlgorithm signatureAlgorithm;
    private final DocumentStampCommand stamp;
    private final char[] documentPassword;
    private final String signerName;
    private final String reason;
    private final String location;

    private DocumentSignCommand(Builder builder) {
        Require.notNull(builder.format, "文档格式不能为空");
        Require.notNull(builder.keyMaterial, "签名密钥不能为空");
        this.format = builder.format;
        this.content = builder.content == null ? null : Arrays.copyOf(builder.content, builder.content.length);
        this.keyMaterial = builder.keyMaterial;
        this.signatureAlgorithm = builder.signatureAlgorithm == null
                ? DocumentSignatureAlgorithm.AUTO : builder.signatureAlgorithm;
        this.stamp = builder.stamp;
        this.documentPassword = builder.documentPassword == null
                ? new char[0] : Arrays.copyOf(builder.documentPassword, builder.documentPassword.length);
        this.signerName = builder.signerName;
        this.reason = builder.reason;
        this.location = builder.location;
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

    public Pkcs12KeyMaterial keyMaterial() {
        return keyMaterial;
    }

    public DocumentSignatureAlgorithm signatureAlgorithm() {
        return signatureAlgorithm;
    }

    public DocumentStampCommand stamp() {
        return stamp;
    }

    public boolean hasStamp() {
        return stamp != null;
    }

    public char[] documentPassword() {
        return Arrays.copyOf(documentPassword, documentPassword.length);
    }

    public String signerName() {
        return signerName;
    }

    public String reason() {
        return reason;
    }

    public String location() {
        return location;
    }

    public static Builder builder() {
        return new Builder();
    }

    @LocalCapabilityContract
    public static final class Builder {

        private DocumentSignFormat format;
        private byte[] content;
        private Pkcs12KeyMaterial keyMaterial;
        private DocumentSignatureAlgorithm signatureAlgorithm = DocumentSignatureAlgorithm.AUTO;
        private DocumentStampCommand stamp;
        private char[] documentPassword;
        private String signerName;
        private String reason;
        private String location;

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

        public Builder keyMaterial(Pkcs12KeyMaterial keyMaterial) {
            this.keyMaterial = keyMaterial;
            return this;
        }

        /**
         * Selects a controlled signature algorithm. The default is {@link DocumentSignatureAlgorithm#AUTO}.
         *
         * @param signatureAlgorithm requested algorithm
         * @return this builder
         */
        public Builder signatureAlgorithm(DocumentSignatureAlgorithm signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public Builder stamp(DocumentStampCommand stamp) {
            this.stamp = stamp;
            return this;
        }

        public Builder documentPassword(char[] documentPassword) {
            this.documentPassword = documentPassword == null
                    ? null : Arrays.copyOf(documentPassword, documentPassword.length);
            return this;
        }

        public Builder signerName(String signerName) {
            this.signerName = signerName;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public DocumentSignCommand build() {
            return new DocumentSignCommand(this);
        }
    }
}
