package io.mango.infra.tools.doc;

/**
 * Converter for one or more source/target document format pairs.
 */
public interface DocumentConverter {

    boolean supports(DocumentFormat sourceFormat, DocumentFormat targetFormat);

    DocumentConvertResult convert(DocumentConvertRequest request);
}
