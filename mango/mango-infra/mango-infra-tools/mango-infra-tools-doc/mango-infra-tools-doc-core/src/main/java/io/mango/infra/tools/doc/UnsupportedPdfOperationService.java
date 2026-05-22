package io.mango.infra.tools.doc;

/**
 * Default PDF operation implementation used until a concrete adapter is added.
 */
public class UnsupportedPdfOperationService implements PdfOperationService {

    @Override
    public PdfOperationResult merge(PdfMergeRequest request) {
        throw new DocumentToolException("PDF merge is not configured");
    }

    @Override
    public PdfOperationResult addWatermark(PdfWatermarkRequest request) {
        throw new DocumentToolException("PDF watermark is not configured");
    }
}
