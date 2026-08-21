package io.mango.infra.docsign.enums;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Supported document signature formats.
 */
@LocalCapabilityContract
public enum DocumentSignFormat {

    /** PDF document with CMS/PKCS#7 signatures. */
    PDF("application/pdf"),

    /** OFD document with OFD signature structures. */
    OFD("application/ofd");

    private final String contentType;

    DocumentSignFormat(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() {
        return contentType;
    }
}
