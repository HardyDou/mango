package io.mango.infra.tools.doc;

import java.util.Set;

/**
 * Entry point for local document tools used by template and file modules.
 */
public interface DocumentToolService {

    boolean canConvert(DocumentFormat sourceFormat, DocumentFormat targetFormat);

    DocumentConvertResult convert(DocumentConvertRequest request);

    Set<DocumentFormatPair> supportedConversions();
}
