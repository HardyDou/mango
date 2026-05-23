package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * 基于 kkFileView/PDFBox 链路的 PDF 首页转图片转换器。
 */
public class PdfToImageConvertProvider implements IConvertProvider {

    private static final int DEFAULT_DPI = 144;

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.PDF
                && (targetFormat == ConvertFormat.PNG || targetFormat == ConvertFormat.JPEG);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        byte[] sourceBytes;
        try {
            sourceBytes = command.inputStream().readAllBytes();
        } catch (Exception ex) {
            throw new ConvertToolException("读取 PDF 内容失败", ex);
        }
        try (PDDocument document = loadDocument(sourceBytes, command);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() <= 0) {
                throw new ConvertToolException("PDF 没有可转换页面");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, dpi(command), ImageType.RGB);
            String imageFormat = command.targetFormat() == ConvertFormat.PNG ? "png" : "jpg";
            ImageIO.write(image, imageFormat, outputStream);
            image.flush();
            return ConvertResultVO.builder()
                    .format(command.targetFormat())
                    .fileName(ConvertFileNames.resolve(command.fileName(), command.targetFormat()))
                    .contentType(command.targetFormat().contentType())
                    .content(outputStream.toByteArray())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("PDF 转图片失败", ex);
        }
    }

    private PDDocument loadDocument(byte[] sourceBytes, ConvertCommand command) throws java.io.IOException {
        String password = password(command);
        if (password == null || password.isBlank()) {
            return Loader.loadPDF(sourceBytes);
        }
        return Loader.loadPDF(sourceBytes, password);
    }

    private String password(ConvertCommand command) {
        Object value = command.options().get(ConvertOptionKeys.PASSWORD);
        return value == null ? null : value.toString();
    }

    private int dpi(ConvertCommand command) {
        Object value = command.options().get(ConvertOptionKeys.DPI);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && !value.toString().isBlank()) {
            return Integer.parseInt(value.toString());
        }
        return DEFAULT_DPI;
    }
}
