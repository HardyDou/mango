package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.convert.OfficeToPdfConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeToPdfConvertProviderTest {

    @Test
    void supports_officeTextAndHtmlToPdf() {
        OfficeToPdfConvertProvider provider = new OfficeToPdfConvertProvider(null);

        assertThat(provider.supports(ConvertFormat.DOCX, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.XLSX, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.PPTX, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.HTML, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.TEXT, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.PDF, ConvertFormat.PNG)).isFalse();
    }
}
