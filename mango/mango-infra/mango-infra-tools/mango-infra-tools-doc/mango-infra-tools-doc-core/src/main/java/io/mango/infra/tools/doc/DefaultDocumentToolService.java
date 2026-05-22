package io.mango.infra.tools.doc;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default local document tool facade.
 */
public class DefaultDocumentToolService implements DocumentToolService {

    private final DocumentToolRegistry registry;

    public DefaultDocumentToolService(DocumentToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean canConvert(DocumentFormat sourceFormat, DocumentFormat targetFormat) {
        if (sourceFormat == null || targetFormat == null) {
            return false;
        }
        if (sourceFormat == targetFormat) {
            return true;
        }
        return registry.findConverter(sourceFormat, targetFormat).isPresent();
    }

    @Override
    public DocumentConvertResult convert(DocumentConvertRequest request) {
        if (request.sourceFormat() == request.targetFormat()) {
            return new SameFormatDocumentConverter().convert(request);
        }
        DocumentConverter converter = registry.findConverter(request.sourceFormat(), request.targetFormat())
                .orElseThrow(() -> new DocumentToolException(
                        "Unsupported document conversion: " + request.sourceFormat() + " -> " + request.targetFormat()));
        return converter.convert(request);
    }

    @Override
    public Set<DocumentFormatPair> supportedConversions() {
        Set<DocumentFormatPair> pairs = new LinkedHashSet<>();
        for (DocumentFormat sourceFormat : DocumentFormat.values()) {
            for (DocumentFormat targetFormat : DocumentFormat.values()) {
                if (sourceFormat != targetFormat && canConvert(sourceFormat, targetFormat)) {
                    pairs.add(new DocumentFormatPair(sourceFormat, targetFormat));
                }
            }
        }
        return pairs;
    }
}
