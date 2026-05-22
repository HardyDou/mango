package io.mango.infra.tools.doc;

import java.io.InputStream;
import java.util.Objects;

/**
 * One PDF input source.
 */
public record PdfSource(String name, InputStream inputStream) {

    public PdfSource {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
    }
}
