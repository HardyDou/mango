package io.mango.infra.fileproc.render;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rectangle;
import com.aspose.pdf.TextFragmentAbsorber;
import com.aspose.pdf.TextFragment;
import io.mango.common.exception.BizException;
import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageEncoding;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageVersion;
import io.mango.infra.fileproc.render.enums.PdfCompressionPreset;
import io.mango.infra.fileproc.render.service.AsposePdfRenderApi;
import io.mango.infra.fileproc.render.service.RenderToolException;
import io.mango.infra.fileproc.render.service.UnsupportedRenderService;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;
import io.mango.infra.fileproc.render.vo.PdfSourceVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfRenderContractTest {

    @BeforeAll
    static void useAsposeCompatibleLocale() {
        Locale.setDefault(Locale.CHINA);
    }

    @Test
    void pdfResultDefensivelyCopiesContent() {
        byte[] content = {1, 2, 3};
        PdfOperationResultVO result = new PdfOperationResultVO("demo.pdf", content);

        content[0] = 9;
        assertThat(result.content()).containsExactly(1, 2, 3);

        byte[] exported = result.content();
        exported[1] = 8;
        assertThat(result.content()).containsExactly(1, 2, 3);
    }

    @Test
    void pdfSourceAndWatermarkRequestRejectNullInputStream() {
        assertThatThrownBy(() -> new PdfSourceVO("demo.pdf", null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 输入源不能为空");

        assertThatThrownBy(() -> new AddPdfWatermarkCommand("demo.pdf", null, "watermark"))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 输入流不能为空");

        assertThatThrownBy(() -> CompressPdfCommand.defaults("demo.pdf", null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 输入流不能为空");
    }

    @Test
    void unsupportedServiceThrowsExplicitRenderException() {
        UnsupportedRenderService service = new UnsupportedRenderService();

        assertThatThrownBy(() -> service.mergePdf(new MergePdfCommand(
                "merged.pdf",
                java.util.List.of(new PdfSourceVO("a.pdf", new ByteArrayInputStream(new byte[0]))),
                true,
                true)))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("PDF 合并能力未配置");

        assertThatThrownBy(() -> service.addPdfWatermark(new AddPdfWatermarkCommand(
                "watermarked.pdf",
                new ByteArrayInputStream(new byte[0]),
                "demo")))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("PDF 水印能力未配置");

        assertThatThrownBy(() -> service.compressPdf(CompressPdfCommand.defaults(
                "compressed.pdf",
                new ByteArrayInputStream(new byte[0]))))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("PDF 压缩能力未配置");

        assertThatThrownBy(() -> service.compressPdfToTarget(new CompressPdfToTargetCommand(
                "compressed.pdf",
                new ByteArrayInputStream(new byte[0]),
                1024,
                PdfCompressionPreset.HIGH,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("PDF 目标压缩能力未配置");
    }

    @Test
    void compressPdfRejectsInvalidFineTuningParameters() {
        assertThatThrownBy(() -> new CompressPdfCommand(
                "demo.pdf",
                new ByteArrayInputStream(new byte[0]),
                PdfCompressionPreset.CUSTOM,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                101,
                null,
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 图片质量必须在 1-100 之间");

        assertThatThrownBy(() -> new CompressPdfCommand(
                "demo.pdf",
                new ByteArrayInputStream(new byte[0]),
                PdfCompressionPreset.CUSTOM,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 图片最大分辨率必须大于 0");
    }

    @Test
    void compressPdfToTargetRejectsInvalidQualityAndTargetParameters() {
        assertThatThrownBy(() -> new CompressPdfToTargetCommand(
                "demo.pdf",
                new ByteArrayInputStream(new byte[0]),
                0,
                PdfCompressionPreset.HIGH,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 目标大小必须大于 0");

        assertThatThrownBy(() -> new CompressPdfToTargetCommand(
                "demo.pdf",
                new ByteArrayInputStream(new byte[0]),
                1024,
                PdfCompressionPreset.HIGH,
                60,
                80,
                null,
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 最低图片质量不能大于首次图片质量");

        assertThatThrownBy(() -> new CompressPdfToTargetCommand(
                "demo.pdf",
                new ByteArrayInputStream(new byte[0]),
                1024,
                PdfCompressionPreset.HIGH,
                null,
                null,
                100,
                200,
                null,
                null,
                null,
                null))
                .isInstanceOf(BizException.class)
                .hasMessage("PDF 最低图片最大分辨率不能大于首次图片最大分辨率");
    }

    @Test
    void asposeRenderApiCompressesPdfWithPresetAndFineTuningParameters() throws Exception {
        byte[] sourcePdf = samplePdfWithImage();
        AsposePdfRenderApi renderApi = new AsposePdfRenderApi(product -> asposeLicenseContent());

        PdfOperationResultVO result = renderApi.compressPdf(new CompressPdfCommand(
                "demo-source.pdf",
                new ByteArrayInputStream(sourcePdf),
                PdfCompressionPreset.HIGH,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                45,
                120,
                PdfCompressionImageEncoding.JPEG,
                PdfCompressionImageVersion.FAST,
                false,
                true,
                true));

        assertThat(result.fileName()).isEqualTo("demo-source.pdf");
        assertThat(result.content()).isNotEmpty();
        Document document = new Document(new ByteArrayInputStream(result.content()));
        try {
            assertThat(document.getPages().size()).isEqualTo(1);
        } finally {
            document.close();
        }
    }

    @Test
    void asposeRenderApiMergesPdfDocuments() throws Exception {
        AsposePdfRenderApi renderApi = new AsposePdfRenderApi(product -> asposeLicenseContent());

        PdfOperationResultVO result = renderApi.mergePdf(new MergePdfCommand(
                "merged-source.doc",
                java.util.List.of(
                        new PdfSourceVO("one.pdf", new ByteArrayInputStream(samplePdf("first page"))),
                        new PdfSourceVO("two.pdf", new ByteArrayInputStream(samplePdf("second page")))),
                false,
                false));

        assertThat(result.fileName()).isEqualTo("merged-source.pdf");
        Document document = new Document(new ByteArrayInputStream(result.content()));
        try {
            assertThat(document.getPages().size()).isEqualTo(2);
            assertThat(extractText(document)).contains("first page");
        } finally {
            document.close();
        }
    }

    @Test
    void asposeRenderApiAddsWatermarkToPdf() throws Exception {
        AsposePdfRenderApi renderApi = new AsposePdfRenderApi(product -> asposeLicenseContent());

        PdfOperationResultVO result = renderApi.addPdfWatermark(new AddPdfWatermarkCommand(
                "",
                new ByteArrayInputStream(samplePdf("watermark source")),
                "mango watermark"));

        assertThat(result.fileName()).isEqualTo("watermarked.pdf");
        Document document = new Document(new ByteArrayInputStream(result.content()));
        try {
            assertThat(document.getPages().size()).isEqualTo(1);
            assertThat(extractText(document)).contains("mango watermark");
        } finally {
            document.close();
        }
    }

    @Test
    void asposeRenderApiCompressesPdfToTargetWithQualityBounds() throws Exception {
        byte[] sourcePdf = samplePdfWithImage();
        AsposePdfRenderApi renderApi = new AsposePdfRenderApi(product -> asposeLicenseContent());

        PdfCompressionResultVO result = renderApi.compressPdfToTarget(new CompressPdfToTargetCommand(
                "target-source.pdf",
                new ByteArrayInputStream(sourcePdf),
                sourcePdf.length - 1L,
                PdfCompressionPreset.HIGH,
                80,
                35,
                220,
                100,
                PdfCompressionImageEncoding.JPEG,
                PdfCompressionImageVersion.FAST,
                4,
                false));

        assertThat(result.fileName()).isEqualTo("target-source.pdf");
        assertThat(result.content()).isNotEmpty();
        assertThat(result.originalSize()).isEqualTo(sourcePdf.length);
        assertThat(result.compressedSize()).isEqualTo(result.content().length);
        assertThat(result.targetSize()).isEqualTo(sourcePdf.length - 1L);
        assertThat(result.iterations()).isBetween(1, 4);
        assertThat(result.finalImageQuality()).isBetween(35, 80);
        assertThat(result.finalResolution()).isBetween(100, 220);
        Document document = new Document(new ByteArrayInputStream(result.content()));
        try {
            assertThat(document.getPages().size()).isEqualTo(1);
        } finally {
            document.close();
        }
    }

    private byte[] samplePdfWithImage() throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            try {
                Page page = document.getPages().add();
                page.getParagraphs().add(new TextFragment("mango render compress pdf"));
                page.addImage(new ByteArrayInputStream(sampleImage()),
                        new Rectangle(40, 320, 560, 760));
                document.save(outputStream);
                return outputStream.toByteArray();
            } finally {
                document.close();
            }
        }
    }

    private byte[] samplePdf(String text) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            try {
                Page page = document.getPages().add();
                page.getParagraphs().add(new TextFragment(text));
                document.save(outputStream);
                return outputStream.toByteArray();
            } finally {
                document.close();
            }
        }
    }

    private String extractText(Document document) {
        TextFragmentAbsorber absorber = new TextFragmentAbsorber();
        document.getPages().accept(absorber);
        return absorber.getText();
    }

    private byte[] sampleImage() throws Exception {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    graphics.setColor(new Color((x * 7) % 255, (y * 5) % 255, ((x + y) * 3) % 255));
                    graphics.fillRect(x, y, 1, 1);
                }
            }
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private byte[] asposeLicenseContent() {
        Path path = Path.of("/Users/hardy/Work/aspose-jar-crack/src/main/resources/license.xml");
        try {
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            return new byte[0];
        } catch (java.io.IOException ex) {
            return new byte[0];
        }
    }
}
