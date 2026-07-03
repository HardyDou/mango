package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 基于 ImageIO/PDFBox 的图片转 PDF 转换器。
 */
public class ImageToPdfConvertProvider implements IConvertProvider {

    private static final float PAGE_MARGIN = 24F;

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return targetFormat == ConvertFormat.PDF
                && (sourceFormat == ConvertFormat.PNG || sourceFormat == ConvertFormat.JPEG);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        try (PDDocument document = new PDDocument();
             OutputStream outputStream = outputStream(command)) {
            BufferedImage image = ImageIO.read(command.inputStream());
            if (image == null) {
                throw new ConvertToolException("图片内容无法读取");
            }
            appendPage(document, image);
            document.save(outputStream);
            image.flush();
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(content(command, outputStream))
                    .outputPath(command.targetPath())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("图片转 PDF 失败", ex);
        }
    }

    private OutputStream outputStream(ConvertCommand command) throws java.io.IOException {
        if (command.hasTargetPath()) {
            ConvertTempFiles.createParent(command.targetPath());
            return Files.newOutputStream(command.targetPath());
        }
        return new ByteArrayOutputStream();
    }

    private byte[] content(ConvertCommand command, OutputStream outputStream) {
        if (command.hasTargetPath()) {
            return new byte[0];
        }
        return ((ByteArrayOutputStream) outputStream).toByteArray();
    }

    private void appendPage(PDDocument document, BufferedImage image) throws java.io.IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
        float[] position = calculateImagePosition(page, pdfImage);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(pdfImage, position[0], position[1], position[2], position[3]);
        }
    }

    private float[] calculateImagePosition(PDPage page, PDImageXObject image) {
        float pageWidth = page.getMediaBox().getWidth() - 2 * PAGE_MARGIN;
        float pageHeight = page.getMediaBox().getHeight() - 2 * PAGE_MARGIN;
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;
        float x = (pageWidth - scaledWidth) / 2 + PAGE_MARGIN;
        float y = (pageHeight - scaledHeight) / 2 + PAGE_MARGIN;
        return new float[]{x, y, scaledWidth, scaledHeight};
    }
}
