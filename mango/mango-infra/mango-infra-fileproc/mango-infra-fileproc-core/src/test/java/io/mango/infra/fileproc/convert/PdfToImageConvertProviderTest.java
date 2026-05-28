package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertOptionKeys;
import io.mango.infra.fileproc.convert.convert.AsposePdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.PdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PdfToImageConvertProviderTest {

    @Test
    void convert_rendersFirstPdfPageToPng() throws Exception {
        PdfToImageConvertProvider provider = new PdfToImageConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PDF)
                .targetFormat(ConvertFormat.PNG)
                .fileName("demo.pdf")
                .inputStream(new ByteArrayInputStream(pdfBytes()))
                .option(ConvertOptionKeys.DPI, 36)
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.PNG);
        assertThat(result.fileName()).isEqualTo("demo.png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.content()).startsWith(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
    }

    @Test
    void supports_onlyPdfToImage() {
        PdfToImageConvertProvider provider = new PdfToImageConvertProvider();

        assertThat(provider.supports(ConvertFormat.PDF, ConvertFormat.PNG)).isTrue();
        assertThat(provider.supports(ConvertFormat.PDF, ConvertFormat.JPEG)).isTrue();
        assertThat(provider.supports(ConvertFormat.PNG, ConvertFormat.PDF)).isFalse();
    }

    @Test
    void asposeConvert_appliesLicenseWhenLocaleContainsHansScript() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("zh-CN-Hans"));
            byte[] licenseContent = Files.readAllBytes(Path.of("src/main/resources/aspose/license.xml"));
            AsposePdfToImageConvertProvider provider = new AsposePdfToImageConvertProvider(licenseContent);

            ConvertResultVO result = provider.convert(ConvertCommand.builder()
                    .sourceFormat(ConvertFormat.PDF)
                    .targetFormat(ConvertFormat.PNG)
                    .fileName("demo.pdf")
                    .inputStream(new ByteArrayInputStream(pdfBytes()))
                    .option(ConvertOptionKeys.DPI, 36)
                    .build());

            assertThat(result.content()).startsWith(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        } finally {
            Locale.setDefault(original);
        }
    }

    private byte[] pdfBytes() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
