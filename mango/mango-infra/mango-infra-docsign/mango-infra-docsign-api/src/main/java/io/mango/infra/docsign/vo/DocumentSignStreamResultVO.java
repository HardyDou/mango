package io.mango.infra.docsign.vo;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignFormat;

/**
 * Metadata returned after a signed document has been written to a caller-owned stream.
 */
@LocalCapabilityContract
public final class DocumentSignStreamResultVO {

    private final DocumentSignFormat format;
    private final long contentLength;
    private final int signatureCount;

    public DocumentSignStreamResultVO(DocumentSignFormat format, long contentLength, int signatureCount) {
        Require.notNull(format, "签名结果格式不能为空");
        Require.isTrue(contentLength > 0, "签名结果长度必须大于 0");
        Require.isTrue(signatureCount > 0, "签名数量必须大于 0");
        this.format = format;
        this.contentLength = contentLength;
        this.signatureCount = signatureCount;
    }

    public DocumentSignFormat format() {
        return format;
    }

    public String contentType() {
        return format.contentType();
    }

    public long contentLength() {
        return contentLength;
    }

    public int signatureCount() {
        return signatureCount;
    }
}
