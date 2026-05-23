package io.mango.infra.fileproc.convert.convert;

import io.mango.infra.fileproc.convert.enums.ConvertFormat;

import java.util.Locale;

/**
 * 转换文件名处理工具。
 */
final class ConvertFileNames {

    private ConvertFileNames() {
    }

    static String resolve(String sourceFileName, ConvertFormat targetFormat) {
        if (sourceFileName == null || sourceFileName.isBlank()) {
            return "converted." + targetFormat.extension();
        }
        String fileName = sourceFileName.trim();
        int index = fileName.lastIndexOf('.');
        String baseName = index > 0 ? fileName.substring(0, index) : fileName;
        return baseName + "." + targetFormat.extension();
    }

    static String extension(String fileName, ConvertFormat format) {
        if (fileName == null || fileName.isBlank()) {
            return format.extension();
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return format.extension();
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
