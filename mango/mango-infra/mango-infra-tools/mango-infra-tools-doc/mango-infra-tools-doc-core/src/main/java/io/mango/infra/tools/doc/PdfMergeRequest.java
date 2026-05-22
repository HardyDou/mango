package io.mango.infra.tools.doc;

import java.util.List;

/**
 * Request for merging multiple PDF inputs.
 */
public record PdfMergeRequest(
        String fileName,
        List<PdfSource> sources,
        boolean rebuildBookmark,
        boolean addPageNumber) {
}
