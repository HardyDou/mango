package io.mango.infra.fileproc.compress.service;

import io.mango.infra.fileproc.compress.enums.FileCompression;

import java.util.List;

/**
 * 压缩档位递增序列。
 * <p>
 * 指定目标体积时，压缩从当前档位开始，逐步提升到更高强度，直到达到目标大小。
 */
final class CompressionLadder {

    private CompressionLadder() {
    }

    static List<FileCompression> from(FileCompression compression) {
        FileCompression resolved = compression == null ? FileCompression.NONE : compression;
        return switch (resolved) {
            case LOW -> List.of(FileCompression.LOW, FileCompression.STANDARD, FileCompression.MEDIUM,
                    FileCompression.HIGH, FileCompression.EXTREME);
            case STANDARD -> List.of(FileCompression.STANDARD, FileCompression.MEDIUM, FileCompression.HIGH,
                    FileCompression.EXTREME);
            case MEDIUM -> List.of(FileCompression.MEDIUM, FileCompression.HIGH, FileCompression.EXTREME);
            case HIGH -> List.of(FileCompression.HIGH, FileCompression.EXTREME);
            case EXTREME -> List.of(FileCompression.EXTREME);
            case NONE -> List.of();
        };
    }
}
