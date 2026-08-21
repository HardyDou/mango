package io.mango.infra.docsign.vo;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignFormat;

import java.util.Arrays;

/**
 * Signed document result.
 */
@LocalCapabilityContract
public final class DocumentSignResultVO {

    private final DocumentSignFormat format;
    private final byte[] content;
    private final int signatureCount;

    public DocumentSignResultVO(DocumentSignFormat format, byte[] content, int signatureCount) {
        Require.notNull(format, "签名结果格式不能为空");
        Require.isTrue(content != null && content.length > 0, "签名结果内容不能为空");
        Require.isTrue(signatureCount > 0, "签名数量必须大于 0");
        this.format = format;
        this.content = Arrays.copyOf(content, content.length);
        this.signatureCount = signatureCount;
    }

    public DocumentSignFormat format() {
        return format;
    }

    public String contentType() {
        return format.contentType();
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public int signatureCount() {
        return signatureCount;
    }
}
