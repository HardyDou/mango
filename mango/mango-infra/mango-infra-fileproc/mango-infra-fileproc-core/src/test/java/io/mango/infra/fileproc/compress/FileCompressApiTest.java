package io.mango.infra.fileproc.compress;

import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.service.DefaultFileCompressApi;
import io.mango.infra.fileproc.compress.service.ImageFileCompressProvider;
import io.mango.infra.fileproc.compress.service.PdfRasterFileCompressProvider;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileCompressApiTest {

    private final FileCompressApi compressApi = new DefaultFileCompressApi(List.of(
            new ImageFileCompressProvider(),
            new PdfRasterFileCompressProvider()));

    @Test
    void compress_图片按档位缩小尺寸并保持图片可读取() throws Exception {
        byte[] source = sampleJpeg(2200, 1600);

        CompressFileResultVO result = compressApi.compress(new CompressFileCommand(
                "photo.jpg",
                "image/jpeg",
                new ByteArrayInputStream(source),
                FileCompression.MEDIUM,
                null));

        assertThat(result.fileName()).isEqualTo("photo.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.content()).isNotEmpty();
        assertThat(result.originalSize()).isEqualTo(source.length);
        BufferedImage compressed = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertThat(Math.max(compressed.getWidth(), compressed.getHeight())).isLessThanOrEqualTo(1450);
        compressed.flush();
    }

    @Test
    void compress_Pdf按栅格化方案输出可读取Pdf() throws Exception {
        byte[] source = samplePdfWithImage();

        CompressFileResultVO result = compressApi.compress(new CompressFileCommand(
                "scan-source.pdf",
                "application/pdf",
                new ByteArrayInputStream(source),
                FileCompression.MEDIUM,
                null));

        assertThat(result.fileName()).isEqualTo("scan-source.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).isNotEmpty();
        assertThat(result.originalSize()).isEqualTo(source.length);
        try (PDDocument document = Loader.loadPDF(result.content())) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void compress_文件已满足目标大小_返回原始内容并标记达成目标() {
        byte[] source = "small".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        CompressFileResultVO result = compressApi.compress(new CompressFileCommand(
                "small.jpg",
                "image/jpeg",
                new ByteArrayInputStream(source),
                FileCompression.MEDIUM,
                100L));

        assertThat(result.content()).containsExactly(source);
        assertThat(result.targetReached()).isTrue();
    }

    private byte[] samplePdfWithImage() throws Exception {
        byte[] image = sampleJpeg(1400, 1000);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                    JPEGFactory.createFromImage(document, bufferedImage, 0.95F);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }
            bufferedImage.flush();
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] sampleJpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int y = 0; y < height; y += 20) {
            for (int x = 0; x < width; x += 20) {
                graphics.setColor(new Color((x * 31) % 255, (y * 17) % 255, ((x + y) * 13) % 255));
                graphics.fillRect(x, y, 20, 20);
            }
        }
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", output);
            image.flush();
            return output.toByteArray();
        }
    }
}
