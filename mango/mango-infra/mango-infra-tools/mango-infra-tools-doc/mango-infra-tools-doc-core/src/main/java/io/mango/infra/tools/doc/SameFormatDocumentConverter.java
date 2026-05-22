package io.mango.infra.tools.doc;

import java.io.IOException;

/**
 * Converter used when source and target formats are identical.
 */
public class SameFormatDocumentConverter implements DocumentConverter {

    @Override
    public boolean supports(DocumentFormat sourceFormat, DocumentFormat targetFormat) {
        return sourceFormat != null && sourceFormat == targetFormat;
    }

    @Override
    public DocumentConvertResult convert(DocumentConvertRequest request) {
        try {
            return DocumentConvertResult.builder()
                    .format(request.targetFormat())
                    .fileName(resolveFileName(request))
                    .contentType(request.targetFormat().contentType())
                    .content(request.inputStream().readAllBytes())
                    .build();
        } catch (IOException ex) {
            throw new DocumentToolException("Failed to copy document content", ex);
        }
    }

    private String resolveFileName(DocumentConvertRequest request) {
        if (request.fileName() == null || request.fileName().isBlank()) {
            return null;
        }
        String fileName = request.fileName().trim();
        String extension = "." + request.targetFormat().extension();
        if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            return fileName;
        }
        return fileName + extension;
    }
}
