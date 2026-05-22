package io.mango.infra.tools.doc;

import java.io.InputStream;
import java.util.Objects;

/**
 * Request for adding PDF watermark.
 */
public record PdfWatermarkRequest(String fileName, InputStream inputStream, String watermarkText) {

    public PdfWatermarkRequest {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
    }
}
