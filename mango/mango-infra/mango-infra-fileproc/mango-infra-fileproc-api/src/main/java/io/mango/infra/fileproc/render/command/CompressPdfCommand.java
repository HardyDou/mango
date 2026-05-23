package io.mango.infra.fileproc.render.command;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageEncoding;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageVersion;
import io.mango.infra.fileproc.render.enums.PdfCompressionPreset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF 压缩命令。
 *
 * @param fileName 输出文件名。
 * @param inputStream PDF 输入流。
 * @param sourcePath PDF 源文件路径。
 * @param targetPath PDF 输出文件路径。
 * @param preset 压缩预设。
 * @param compressObjects 是否压缩 PDF 对象。
 * @param linkDuplicateStreams 是否链接重复流。
 * @param allowReusePageContent 是否复用页面内容。
 * @param removeUnusedStreams 是否移除未使用流。
 * @param removeUnusedObjects 是否移除未使用对象。
 * @param compressImages 是否压缩图片。
 * @param resizeImages 是否缩放图片。
 * @param imageQuality 图片质量，范围 1-100。
 * @param maxResolution 最大图片分辨率 DPI。
 * @param imageEncoding 图片编码策略。
 * @param imageVersion 图片压缩算法版本。
 * @param unembedFonts 是否反嵌入字体。
 * @param subsetFonts 是否子集化字体。
 * @param removePrivateInfo 是否移除私有信息。
 */
public record CompressPdfCommand(
        String fileName,
        InputStream inputStream,
        Path sourcePath,
        Path targetPath,
        PdfCompressionPreset preset,
        Boolean compressObjects,
        Boolean linkDuplicateStreams,
        Boolean allowReusePageContent,
        Boolean removeUnusedStreams,
        Boolean removeUnusedObjects,
        Boolean compressImages,
        Boolean resizeImages,
        Integer imageQuality,
        Integer maxResolution,
        PdfCompressionImageEncoding imageEncoding,
        PdfCompressionImageVersion imageVersion,
        Boolean unembedFonts,
        Boolean subsetFonts,
        Boolean removePrivateInfo) {

    public CompressPdfCommand {
        Require.isTrue(inputStream != null || sourcePath != null, "PDF 输入流或源文件路径不能为空");
        if (imageQuality != null) {
            Require.isTrue(imageQuality >= 1 && imageQuality <= 100, "PDF 图片质量必须在 1-100 之间");
        }
        if (maxResolution != null) {
            Require.isTrue(maxResolution > 0, "PDF 图片最大分辨率必须大于 0");
        }
    }

    public static CompressPdfCommand defaults(String fileName, InputStream inputStream) {
        return new CompressPdfCommand(fileName, inputStream, null, null, PdfCompressionPreset.DEFAULT,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public CompressPdfCommand(
            String fileName,
            InputStream inputStream,
            PdfCompressionPreset preset,
            Boolean compressObjects,
            Boolean linkDuplicateStreams,
            Boolean allowReusePageContent,
            Boolean removeUnusedStreams,
            Boolean removeUnusedObjects,
            Boolean compressImages,
            Boolean resizeImages,
            Integer imageQuality,
            Integer maxResolution,
            PdfCompressionImageEncoding imageEncoding,
            PdfCompressionImageVersion imageVersion,
            Boolean unembedFonts,
            Boolean subsetFonts,
            Boolean removePrivateInfo) {
        this(fileName, inputStream, null, null, preset, compressObjects, linkDuplicateStreams, allowReusePageContent,
                removeUnusedStreams, removeUnusedObjects, compressImages, resizeImages, imageQuality, maxResolution,
                imageEncoding, imageVersion, unembedFonts, subsetFonts, removePrivateInfo);
    }

    public static CompressPdfCommand defaults(Path sourcePath, Path targetPath) {
        return new CompressPdfCommand(null, null, sourcePath, targetPath, PdfCompressionPreset.DEFAULT,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public InputStream inputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        try {
            return Files.newInputStream(sourcePath);
        } catch (IOException ex) {
            throw new IllegalStateException("打开 PDF 源文件失败: " + sourcePath, ex);
        }
    }
}
