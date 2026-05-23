package io.mango.infra.fileproc.render.command;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageEncoding;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageVersion;
import io.mango.infra.fileproc.render.enums.PdfCompressionPreset;

import java.io.InputStream;

/**
 * PDF 目标大小压缩命令。
 *
 * @param fileName 输出文件名。
 * @param inputStream PDF 输入流。
 * @param targetSizeBytes 目标大小，单位字节。
 * @param initialPreset 初始压缩预设。
 * @param preferredImageQuality 首次尝试图片质量，范围 1-100。
 * @param minImageQuality 最低图片质量，范围 1-100。
 * @param preferredResolution 首次尝试最大图片分辨率 DPI。
 * @param minResolution 最低最大图片分辨率 DPI。
 * @param imageEncoding 图片编码策略。
 * @param imageVersion 图片压缩算法版本。
 * @param maxIterations 最大尝试次数。
 * @param strictTarget 是否必须达到目标大小。
 */
public record CompressPdfToTargetCommand(
        String fileName,
        InputStream inputStream,
        long targetSizeBytes,
        PdfCompressionPreset initialPreset,
        Integer preferredImageQuality,
        Integer minImageQuality,
        Integer preferredResolution,
        Integer minResolution,
        PdfCompressionImageEncoding imageEncoding,
        PdfCompressionImageVersion imageVersion,
        Integer maxIterations,
        Boolean strictTarget) {

    public CompressPdfToTargetCommand {
        Require.notNull(inputStream, "PDF 输入流不能为空");
        Require.isTrue(targetSizeBytes > 0, "PDF 目标大小必须大于 0");
        validateQuality(preferredImageQuality, "PDF 首次图片质量必须在 1-100 之间");
        validateQuality(minImageQuality, "PDF 最低图片质量必须在 1-100 之间");
        validateResolution(preferredResolution, "PDF 首次图片最大分辨率必须大于 0");
        validateResolution(minResolution, "PDF 最低图片最大分辨率必须大于 0");
        if (preferredImageQuality != null && minImageQuality != null) {
            Require.isTrue(minImageQuality <= preferredImageQuality,
                    "PDF 最低图片质量不能大于首次图片质量");
        }
        if (preferredResolution != null && minResolution != null) {
            Require.isTrue(minResolution <= preferredResolution,
                    "PDF 最低图片最大分辨率不能大于首次图片最大分辨率");
        }
        if (maxIterations != null) {
            Require.isTrue(maxIterations > 0, "PDF 最大压缩次数必须大于 0");
        }
    }

    private static void validateQuality(Integer quality, String message) {
        if (quality != null) {
            Require.isTrue(quality >= 1 && quality <= 100, message);
        }
    }

    private static void validateResolution(Integer resolution, String message) {
        if (resolution != null) {
            Require.isTrue(resolution > 0, message);
        }
    }
}
