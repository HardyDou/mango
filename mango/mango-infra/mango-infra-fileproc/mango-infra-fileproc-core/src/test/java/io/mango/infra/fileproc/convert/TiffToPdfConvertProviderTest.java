package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.TiffToPdfConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TiffToPdfConvertProviderTest {

    @Test
    void convert_convertsTiffToPdf() throws Exception {
        TiffToPdfConvertProvider provider = new TiffToPdfConvertProvider();

        ConvertResultVO result = provider.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TIFF)
                .targetFormat(ConvertFormat.PDF)
                .fileName("scan.tif")
                .inputStream(new ByteArrayInputStream(tiffBytes()))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.fileName()).isEqualTo("scan.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void supports_onlyTiffToPdf() {
        TiffToPdfConvertProvider provider = new TiffToPdfConvertProvider();

        assertThat(provider.supports(ConvertFormat.TIFF, ConvertFormat.PDF)).isTrue();
        assertThat(provider.supports(ConvertFormat.PNG, ConvertFormat.PDF)).isFalse();
    }

    private byte[] tiffBytes() throws Exception {
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 80, 40);
        graphics.setColor(Color.BLACK);
        graphics.drawString("mango", 10, 20);
        graphics.dispose();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, "tif", outputStream);
            assertThat(written).isTrue();
            return outputStream.toByteArray();
        }
    }
}
