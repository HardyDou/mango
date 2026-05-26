package cn.keking.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompressFileEntryServiceTest {

    private final CompressFileEntryService service = new CompressFileEntryService();

    @Test
    void contentType_whenPdfEntry_returnsPdfMimeType() {
        assertThat(service.contentType("sample.pdf", "archive_/sample.pdf"))
                .isEqualTo("application/pdf");
    }

    @Test
    void resolveEntryPath_whenEntryDoesNotBelongToArchive_rejectsPath() {
        assertThatThrownBy(() -> service.resolveEntryPath("archive_", "other_/sample.pdf"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void resolveEntryPath_whenEntryContainsTraversal_rejectsPath() {
        assertThatThrownBy(() -> service.resolveEntryPath("archive_", "archive_/../sample.pdf"))
                .isInstanceOf(SecurityException.class);
    }
}
