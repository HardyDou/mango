package io.mango.infra.tools.doc;

import java.util.Arrays;

/**
 * Result of one PDF operation.
 */
public record PdfOperationResult(String fileName, byte[] content) {

    public PdfOperationResult {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
