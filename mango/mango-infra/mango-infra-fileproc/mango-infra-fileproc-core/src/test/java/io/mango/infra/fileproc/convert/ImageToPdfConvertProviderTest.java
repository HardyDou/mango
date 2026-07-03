package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ImageToPdfConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImageToPdfConvertProviderTest {

    @Test
    void convert_convertsPngToPdf() throws Exception {
        ImageToPdfConvertProvider provider = new ImageToPdfConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PNG)
                .targetFormat(ConvertFormat.PDF)
                .fileName("scan.png")
                .inputStream(new ByteArrayInputStream(imageBytes("png")))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.fileName()).isEqualTo("scan.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void convert_convertsJpegToPdf() throws Exception {
        ImageToPdfConvertProvider provider = new ImageToPdfConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.JPEG)
                .targetFormat(ConvertFormat.PDF)
                .fileName("photo.jpg")
                .inputStream(new ByteArrayInputStream(imageBytes("jpeg")))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.fileName()).isEqualTo("photo.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void supports_onlyPngAndJpegToPdf() {
        ImageToPdfConvertProvider provider = new ImageToPdfConvertProvider();

        assertThat(provider.supports(ConvertFormat.PNG, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.JPEG, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.TIFF, ConvertFormat.PDF)).isFalse();
        assertThat(provider.supports(ConvertFormat.PNG, ConvertFormat.JPEG)).isFalse();
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 120, 80);
            graphics.setColor(Color.BLACK);
            graphics.drawString("mango", 20, 42);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, format, outputStream);
            assertThat(written).isTrue();
            return outputStream.toByteArray();
        }
    }
}
