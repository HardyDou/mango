package io.mango.infra.tools.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * In-memory converter registry.
 */
public class DocumentToolRegistry {

    private final List<DocumentConverter> converters = new ArrayList<>();

    public DocumentToolRegistry(Collection<DocumentConverter> converters) {
        if (converters != null) {
            this.converters.addAll(converters);
        }
    }

    public Optional<DocumentConverter> findConverter(DocumentFormat sourceFormat, DocumentFormat targetFormat) {
        return converters.stream()
                .filter(converter -> converter.supports(sourceFormat, targetFormat))
                .findFirst();
    }

    public List<DocumentConverter> converters() {
        return List.copyOf(converters);
    }
}
