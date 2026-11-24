package io.mango.file.preview.core.service.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewModeTest {

    @Test
    void sheetStaysEngineRenderedWhenInternalSourceIsAvailable() {
        // 直取通道下表格页的取数地址换成源文件落盘后的同源地址，不再回落 PDF 转换。
        assertThat(FilePreviewMode.of("xlsx", true)).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(FilePreviewMode.of("CSV", true)).isEqualTo(FilePreviewMode.ENGINE);
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
