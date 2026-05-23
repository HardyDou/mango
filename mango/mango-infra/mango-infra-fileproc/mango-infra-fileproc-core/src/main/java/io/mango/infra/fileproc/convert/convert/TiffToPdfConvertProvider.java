package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.commons.imaging.Imaging;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 基于 kkFileView/Commons Imaging/PDFBox 链路的 TIFF 转 PDF 转换器。
 */
public class TiffToPdfConvertProvider implements IConvertProvider {

    private static final float PAGE_MARGIN = 5F;

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.TIFF && targetFormat == ConvertFormat.PDF;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        Path workDir = ConvertTempFiles.createWorkDir();
        try {
            Path inputFile = ConvertTempFiles.writeInput(workDir, command);
            List<BufferedImage> images = Imaging.getAllBufferedImages(inputFile.toFile());
            if (images.isEmpty()) {
                throw new ConvertToolException("TIFF 文件没有可转换页面");
            }
            try (PDDocument document = new PDDocument();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                for (BufferedImage image : images) {
                    appendPage(document, processImage(image));
                }
                document.save(outputStream);
                return ConvertResultVO.builder()
                        .format(ConvertFormat.PDF)
                        .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                        .contentType(ConvertFormat.PDF.contentType())
                        .content(outputStream.toByteArray())
                        .build();
            }
        } catch (Exception ex) {
            throw new ConvertToolException("TIFF 转 PDF 失败", ex);
        } finally {
            ConvertTempFiles.deleteQuietly(workDir);
        }
    }

    private void appendPage(PDDocument document, BufferedImage image) throws java.io.IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
        float[] position = calculateImagePosition(page, pdImage);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(pdImage, position[0], position[1], position[2], position[3]);
        }
        image.flush();
    }

    private BufferedImage processImage(BufferedImage original) {
        int targetDpi = 150;
        float a4WidthInch = 8.27F;
        float a4HeightInch = 11.69F;
        int maxWidth = (int) (a4WidthInch * targetDpi);
        int maxHeight = (int) (a4HeightInch * targetDpi);
        if (original.getWidth() <= maxWidth && original.getHeight() <= maxHeight) {
            return original;
        }
        double scale = Math.min((double) maxWidth / original.getWidth(), (double) maxHeight / original.getHeight());
        int newWidth = (int) (original.getWidth() * scale);
        int newHeight = (int) (original.getHeight() * scale);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.drawImage(original, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return resized;
    }

    private float[] calculateImagePosition(PDPage page, PDImageXObject pdImage) {
        float pageWidth = page.getMediaBox().getWidth() - 2 * PAGE_MARGIN;
        float pageHeight = page.getMediaBox().getHeight() - 2 * PAGE_MARGIN;
        float imageWidth = pdImage.getWidth();
        float imageHeight = pdImage.getHeight();
        float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;
        float x = (pageWidth - scaledWidth) / 2 + PAGE_MARGIN;
        float y = (pageHeight - scaledHeight) / 2 + PAGE_MARGIN;
        return new float[]{x, y, scaledWidth, scaledHeight};
    }
}
