package io.mango.infra.fileproc.compress.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 基于 PDFBox 的 PDF 栅格化压缩实现。
 */
public class PdfRasterFileCompressProvider implements IFileCompressProvider {

    @Override
    public boolean supports(String fileName, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals("application/pdf") || "pdf".equals(FileCompressionNames.extension(fileName));
    }

    @Override
    public CompressFileResultVO compress(CompressFileCommand command) {
        Require.notNull(command, "PDF 压缩命令不能为空");
        byte[] source = command.readAllBytes();
        if (source.length == 0 || sourceAlreadyWithinTarget(source, command)) {
            return result(command, source, source, targetReached(source.length, command));
        }
        try {
            byte[] best = source;
            boolean reached = targetReached(best.length, command);
            for (CompressionSettings settings : attempts(command.resolvedCompression(), command.targetSizeBytes())) {
                byte[] compressed = compressPdf(source, settings);
                if (compressed.length < best.length) {
                    best = compressed;
                }
                if (targetReached(compressed.length, command)) {
                    reached = true;
                    best = compressed;
                    break;
                }
            }
            return result(command, source, best, reached);
        } catch (CompressionToolException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CompressionToolException("PDF 压缩失败", ex);
        }
    }

    private boolean sourceAlreadyWithinTarget(byte[] source, CompressFileCommand command) {
        return command.targetSizeBytes() != null && source.length <= command.targetSizeBytes();
    }

    private List<CompressionSettings> attempts(FileCompression compression, Long targetSizeBytes) {
        if (targetSizeBytes == null) {
            return List.of(CompressionSettings.of(compression));
        }
        List<CompressionSettings> result = new ArrayList<>();
        switch (compression) {
            case LOW -> {
                result.add(CompressionSettings.of(FileCompression.LOW));
                result.add(CompressionSettings.of(FileCompression.MEDIUM));
                result.add(CompressionSettings.of(FileCompression.HIGH));
            }
            case MEDIUM -> {
                result.add(CompressionSettings.of(FileCompression.MEDIUM));
                result.add(CompressionSettings.of(FileCompression.HIGH));
            }
            case HIGH -> result.add(CompressionSettings.of(FileCompression.HIGH));
            case NONE -> {
            }
        }
        return result;
    }

    private byte[] compressPdf(byte[] source, CompressionSettings settings) throws java.io.IOException {
        try (PDDocument sourceDocument = Loader.loadPDF(source);
             PDDocument targetDocument = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(sourceDocument);
            for (int pageIndex = 0; pageIndex < sourceDocument.getNumberOfPages(); pageIndex++) {
                BufferedImage rendered = renderer.renderImageWithDPI(pageIndex, settings.pdfRenderDpi(), ImageType.RGB);
                BufferedImage resized = resize(rendered, settings.maxSidePixels());
                PDRectangle mediaBox = sourceDocument.getPage(pageIndex).getMediaBox();
                PDPage page = new PDPage(mediaBox);
                targetDocument.addPage(page);
                PDImageXObject image = JPEGFactory.createFromImage(targetDocument, resized, settings.imageQuality());
                try (PDPageContentStream contentStream = new PDPageContentStream(targetDocument, page)) {
                    contentStream.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                }
                rendered.flush();
                resized.flush();
            }
            targetDocument.save(output);
            return output.toByteArray();
        }
    }

    private BufferedImage resize(BufferedImage source, int maxSidePixels) {
        int max = Math.max(source.getWidth(), source.getHeight());
        if (max <= maxSidePixels) {
            return source;
        }
        double scale = maxSidePixels / (double) max;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private CompressFileResultVO result(CompressFileCommand command, byte[] source, byte[] output, boolean targetReached) {
        return new CompressFileResultVO(fileName(command), "application/pdf", output, source.length, output.length,
                command.targetSizeBytes(), targetReached);
    }

    private String fileName(CompressFileCommand command) {
        return StringUtils.hasText(command.fileName()) ? FileCompressionNames.pdfFileName(command.fileName()) : "compressed.pdf";
    }

    private boolean targetReached(long size, CompressFileCommand command) {
        return command.targetSizeBytes() == null || size <= command.targetSizeBytes();
    }
}
