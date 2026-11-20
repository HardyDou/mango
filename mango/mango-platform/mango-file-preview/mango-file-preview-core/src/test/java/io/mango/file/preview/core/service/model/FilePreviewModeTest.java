package io.mango.file.preview.core.service.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewModeTest {

    @Test
    void sheetFallsBackToPdfConversionWhenInternalSourceIsAvailable() {
        assertThat(FilePreviewMode.of("xlsx", true)).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(FilePreviewMode.of("CSV", true)).isEqualTo(FilePreviewMode.PDF_CONVERSION);
    }

    @Test
    void sheetStaysEngineRenderedWhenInternalSourceIsUnavailable() {
        assertThat(FilePreviewMode.of("xlsx", false)).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(FilePreviewMode.of("csv", false)).isEqualTo(FilePreviewMode.ENGINE);
    }

    @Test
    void nonSheetExtensionsIgnoreInternalSourceAvailability() {
        assertThat(FilePreviewMode.of("pdf", true)).isEqualTo(FilePreviewMode.NATIVE_PDF);
        assertThat(FilePreviewMode.of("docx", true)).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(FilePreviewMode.of("zip", true)).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(FilePreviewMode.of("png", true)).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(FilePreviewMode.of("", true)).isEqualTo(FilePreviewMode.ENGINE);
    }
}
