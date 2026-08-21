package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.awt.RenderingHints;
import java.nio.file.Path;

/**
 * 基于 OFDRW 的 PDF 转 OFD 转换器。
 *
 * <p>OFDRW 的 PDF 转换路径使用 PDFBox 渲染 PDF 页面后写入 OFD，
 * 因此输出保留页面视觉效果，但不会把 PDF 交互式表单或原生数字签名转换为 OFD 语义对象。</p>
 */
public class PdfToOfdConvertProvider implements IConvertProvider {

    private static final double PDF_USER_UNITS_PER_MILLIMETRE = 72D / 25.4D;

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.PDF && targetFormat == ConvertFormat.OFD;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        Path workDir = ConvertTempFiles.createWorkDir();
        try {
            Path inputFile = ConvertTempFiles.writeInput(workDir, command);
            Path outputFile = ConvertTempFiles.output(workDir, command, ConvertFormat.OFD);
            renderPdf(inputFile, outputFile, command);
            return ConvertResultVO.builder()
                    .format(ConvertFormat.OFD)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.OFD))
                    .contentType(ConvertFormat.OFD.contentType())
                    .content(ConvertTempFiles.readIfNeeded(command, outputFile))
                    .outputPath(command.targetPath())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("PDF 转 OFD 失败", ex);
        } finally {
            ConvertTempFiles.deleteQuietly(workDir);
        }
    }

    private void renderPdf(Path inputFile, Path outputFile, ConvertCommand command) throws java.io.IOException {
        try (PDDocument pdfDocument = loadPdf(inputFile, command);
             OFDGraphicsDocument ofdDocument = new OFDGraphicsDocument(outputFile)) {
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            renderer.setRenderingHints(renderingHints());
            for (int pageIndex = 0; pageIndex < pdfDocument.getNumberOfPages(); pageIndex++) {
                renderPage(pdfDocument, renderer, ofdDocument, pageIndex);
            }
        }
    }

    private PDDocument loadPdf(Path inputFile, ConvertCommand command) throws java.io.IOException {
        Object password = command.options().get(ConvertOptionKeys.PASSWORD);
        if (password == null || password.toString().isBlank()) {
            return Loader.loadPDF(inputFile.toFile());
        }
        return Loader.loadPDF(inputFile.toFile(), password.toString());
    }

    private void renderPage(PDDocument pdfDocument, PDFRenderer renderer,
                            OFDGraphicsDocument ofdDocument, int pageIndex) throws java.io.IOException {
        PDPage pdfPage = pdfDocument.getPage(pageIndex);
        PDRectangle pageBox = pdfPage.getCropBox();
        double width = pageBox.getWidth();
        double height = pageBox.getHeight();
        int rotation = Math.floorMod(pdfPage.getRotation(), 360);
        if (rotation == 90 || rotation == 270) {
            double originalWidth = width;
            width = height;
            height = originalWidth;
        }
        OFDPageGraphics2D graphics = ofdDocument.newPage(
                width / PDF_USER_UNITS_PER_MILLIMETRE,
                height / PDF_USER_UNITS_PER_MILLIMETRE);
        try {
            renderer.renderPageToGraphics(pageIndex, graphics);
        } finally {
            graphics.dispose();
        }
    }

    private RenderingHints renderingHints() {
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return hints;
    }
}
