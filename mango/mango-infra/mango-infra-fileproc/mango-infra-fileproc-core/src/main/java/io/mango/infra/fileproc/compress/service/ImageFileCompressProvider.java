package io.mango.infra.fileproc.compress.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import org.springframework.util.StringUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 基于 ImageIO 的图片压缩实现。
 */
public class ImageFileCompressProvider implements IFileCompressProvider {

    private static final List<String> EXTENSIONS = List.of("jpg", "jpeg", "png");

    @Override
    public boolean supports(String fileName, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals("image/jpeg")
                || type.equals("image/jpg")
                || type.equals("image/png")
                || EXTENSIONS.contains(FileCompressionNames.extension(fileName));
    }

    @Override
    public CompressFileResultVO compress(CompressFileCommand command) {
        Require.notNull(command, "图片压缩命令不能为空");
        byte[] source = command.readAllBytes();
        if (source.length == 0 || sourceAlreadyWithinTarget(source, command)) {
            return result(command, source, source, targetReached(source.length, command));
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
            if (image == null) {
                throw new CompressionToolException("图片内容无法读取");
            }
            byte[] best = source;
            boolean reached = targetReached(best.length, command);
            for (CompressionSettings settings : attempts(command.resolvedCompression(), command.targetSizeBytes())) {
                byte[] compressed = compressImage(image, imageFormat(command), settings);
                if (compressed.length < best.length) {
                    best = compressed;
                }
                if (targetReached(compressed.length, command)) {
                    reached = true;
                    best = compressed;
                    break;
                }
            }
            image.flush();
            return result(command, source, best, reached);
        } catch (CompressionToolException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CompressionToolException("图片压缩失败", ex);
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

    private byte[] compressImage(BufferedImage source, String format, CompressionSettings settings) throws java.io.IOException {
        BufferedImage resized = resize(source, settings.maxSidePixels(), "jpg".equals(format));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if ("jpg".equals(format)) {
                writeJpeg(resized, settings.imageQuality(), output);
            } else {
                ImageIO.write(resized, format, output);
            }
            resized.flush();
            return output.toByteArray();
        }
    }

    private BufferedImage resize(BufferedImage source, int maxSidePixels, boolean rgb) {
        int max = Math.max(source.getWidth(), source.getHeight());
        if (max <= maxSidePixels) {
            return normalizeImage(source, rgb);
        }
        double scale = maxSidePixels / (double) max;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height,
                rgb ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        if (rgb) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private BufferedImage normalizeImage(BufferedImage source, boolean rgb) {
        int type = rgb ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        if (source.getType() == type) {
            return source;
        }
        BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), type);
        Graphics2D graphics = normalized.createGraphics();
        if (rgb) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        }
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return normalized;
    }

    private void writeJpeg(BufferedImage image, float quality, ByteArrayOutputStream output) throws java.io.IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", output);
            return;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private String imageFormat(CompressFileCommand command) {
        String ext = FileCompressionNames.extension(command.fileName());
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "jpg";
        }
        if ("png".equals(ext)) {
            return "png";
        }
        String type = command.normalizedContentType();
        if (type.equals("image/jpeg") || type.equals("image/jpg")) {
            return "jpg";
        }
        return "png";
    }

    private CompressFileResultVO result(CompressFileCommand command, byte[] source, byte[] output, boolean targetReached) {
        return new CompressFileResultVO(fileName(command), contentType(command), output, source.length, output.length,
                command.targetSizeBytes(), targetReached);
    }

    private String fileName(CompressFileCommand command) {
        return StringUtils.hasText(command.fileName()) ? command.fileName() : "compressed." + imageFormat(command);
    }

    private String contentType(CompressFileCommand command) {
        if (StringUtils.hasText(command.contentType())) {
            return command.contentType();
        }
        return "jpg".equals(imageFormat(command)) ? "image/jpeg" : "image/png";
    }

    private boolean targetReached(long size, CompressFileCommand command) {
        return command.targetSizeBytes() == null || size <= command.targetSizeBytes();
    }
}
