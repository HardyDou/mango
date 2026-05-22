package io.mango.infra.tools.doc;

/**
 * Local PDF operations. Concrete implementations can be backed by PDFBox,
 * iText, Aspose, or another local library.
 */
public interface PdfOperationService {

    PdfOperationResult merge(PdfMergeRequest request);

    PdfOperationResult addWatermark(PdfWatermarkRequest request);
}
