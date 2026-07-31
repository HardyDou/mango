package io.mango.file.core.service.impl;

import io.mango.common.result.Require;
import io.mango.file.api.command.FilePackageEntryCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.enums.FilePackageSizeControlMode;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePackageEntryResultVO;
import io.mango.file.api.vo.FilePackageResultVO;
import io.mango.infra.fileproc.compress.FileCompressApi;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 构建带大小控制的单个 ZIP，并记录逐条目压缩结果。
 */
@Component
@Slf4j
final class FilePackageSizeControlProcessor {

    private static final int MAX_AUTO_ROUNDS = 16;
    private static final String ZIP_FILE_NAME = "package.zip";
    private static final String BEST_ZIP_FILE_NAME = "package-best.zip";

    private final List<FileCompressApi> fileCompressApis;

    FilePackageSizeControlProcessor(List<FileCompressApi> fileCompressApis) {
        this.fileCompressApis = fileCompressApis == null ? List.of() : List.copyOf(fileCompressApis);
    }

    PackageBuild process(FilePackageSizeControlCommand command,
                         Function<Long, FileDownloadVO> downloadResolver) {
        FilePackageSizeControlCommand requiredCommand = Require.nonNull(command, FileCode.FILE_EMPTY);
        Require.notEmpty(requiredCommand.getEntries(), FileCode.FILE_EMPTY);
        FilePackageSizeControlMode mode = Require.nonNull(
                requiredCommand.getSizeControlMode(), FileCode.FILE_COMPRESSION_INVALID);
        if (mode == FilePackageSizeControlMode.AUTO) {
            Require.isTrue(requiredCommand.getMaxPackageSizeBytes() != null
                    && requiredCommand.getMaxPackageSizeBytes() > 0, FileCode.FILE_COMPRESSION_INVALID);
        }

        Path workDirectory = createWorkDirectory();
        boolean completed = false;
        try {
            List<EntryWork> entries = prepareEntries(requiredCommand, downloadResolver, workDirectory);
            if (mode == FilePackageSizeControlMode.MANUAL) {
                compressManualEntries(entries, workDirectory);
            } else {
                initializeAutomaticEntries(entries);
            }
            Path zipPath = workDirectory.resolve(ZIP_FILE_NAME);
            long zipSize = writeZip(zipPath, entries);
            PackageSnapshot best = snapshot(zipPath, zipSize, entries, workDirectory);
            if (mode == FilePackageSizeControlMode.AUTO
                    && zipSize > requiredCommand.getMaxPackageSizeBytes()) {
                best = compressAutomatic(requiredCommand.getMaxPackageSizeBytes(), entries,
                        zipPath, best, workDirectory);
            }
            FilePackageResultVO result = result(requiredCommand, best);
            completed = true;
            return new PackageBuild(workDirectory, best.zipPath(), result);
        } finally {
            if (!completed) {
                deleteRecursively(workDirectory);
            }
        }
    }

    private Path createWorkDirectory() {
        try {
            return Files.createTempDirectory("mango-file-package-");
        } catch (IOException ex) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }
    }

    private List<EntryWork> prepareEntries(FilePackageSizeControlCommand command,
                                           Function<Long, FileDownloadVO> downloadResolver,
                                           Path workDirectory) {
        List<EntryWork> result = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        int index = 0;
        for (FilePackageEntryCommand entryValue : command.getEntries()) {
            FilePackageEntryCommand entry = Require.nonNull(entryValue, FileCode.FILE_EMPTY);
            Long fileId = Require.nonNull(entry.getFileId(), FileCode.FILE_NOT_FOUND);
            FileDownloadVO download = Require.nonNull(downloadResolver.apply(fileId), FileCode.FILE_NOT_FOUND);
            InputStream downloadStream = Require.nonNull(download.inputStream(), FileCode.FILE_READ_FAILED);
            String path = FileService.normalizeZipEntryPath(
                    FileService.resolveZipEntryPath(entry.getPath(), download.fileName()));
            Require.isTrue(paths.add(path), FileCode.FILE_NAME_DUPLICATED);
            Path sourcePath = workDirectory.resolve("entry-" + index + "-source");
            copySource(downloadStream, sourcePath);
            FileCompression compression = resolveCompression(entryCompression(command, entry));
            FileCompressApi compressApi = supportedApi(download);
            result.add(new EntryWork(index,
                    fileId,
                    path,
                    download.fileName(),
                    download.contentType(),
                    sourcePath,
                    Files.exists(sourcePath) ? fileSize(sourcePath) : 0L,
                    compression,
                    compressApi,
                    entry.getTargetSizeBytes()));
            index++;
        }
        return result;
    }

    private void copySource(InputStream inputStream, Path target) {
        try (InputStream input = inputStream) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private FileCompression resolveCompression(String value) {
        try {
            return FileCompression.of(value);
        } catch (IllegalArgumentException ex) {
            return Require.fail(FileCode.FILE_COMPRESSION_INVALID);
        }
    }

    private String entryCompression(FilePackageSizeControlCommand command, FilePackageEntryCommand entry) {
        return StringUtils.hasText(entry.getCompression()) ? entry.getCompression() : command.getCompression();
    }

    private FileCompressApi supportedApi(FileDownloadVO download) {
        return fileCompressApis.stream()
                .filter(api -> api.supports(download.fileName(), download.contentType()))
                .findFirst()
                .orElse(null);
    }

    private void compressManualEntries(List<EntryWork> entries, Path workDirectory) {
        for (EntryWork entry : entries) {
            Long targetSize = entry.requestedTargetSize();
            entry.setAssignedTargetSize(targetSize);
            if (targetSize == null) {
                entry.setMessage("未设置手动目标，保持原文件");
                continue;
            }
            if (!entry.compression().enabled()) {
                entry.setTargetAchieved(entry.currentSize() <= targetSize);
                entry.setMessage("compression=NONE，保持原文件");
                continue;
            }
            if (entry.compressApi() == null) {
                entry.setTargetAchieved(entry.currentSize() <= targetSize);
                entry.setMessage("当前格式不支持压缩，保持原文件");
                continue;
            }
            CompressionAttempt attempt = compressEntry(entry, targetSize, workDirectory, 0);
            entry.setActive(false);
            entry.setTargetAchieved(entry.currentSize() <= targetSize);
            entry.setMessage(attemptMessage(attempt, entry.getTargetAchieved()));
        }
    }

    private void initializeAutomaticEntries(List<EntryWork> entries) {
        entries.forEach(entry -> {
            boolean active = entry.compression().enabled() && entry.compressApi() != null && entry.currentSize() > 1;
            entry.setActive(active);
            if (!entry.compression().enabled()) {
                entry.setMessage("compression=NONE，保持原文件");
            } else if (entry.compressApi() == null) {
                entry.setMessage("当前格式不支持压缩，保持原文件");
            } else {
                entry.setMessage("最终ZIP达到目标时无需压缩");
            }
        });
    }

    private PackageSnapshot compressAutomatic(long maxPackageSizeBytes,
                                              List<EntryWork> entries,
                                              Path zipPath,
                                              PackageSnapshot best,
                                              Path workDirectory) {
        int round = 1;
        long currentZipSize = best.zipSize();
        while (currentZipSize > maxPackageSizeBytes && round <= MAX_AUTO_ROUNDS) {
            List<EntryWork> activeEntries = entries.stream().filter(EntryWork::isActive).toList();
            if (activeEntries.isEmpty()) {
                break;
            }
            long requiredSaving = currentZipSize - maxPackageSizeBytes;
            long totalActiveSize = activeEntries.stream().mapToLong(EntryWork::currentSize).sum();
            boolean reduced = false;
            for (EntryWork entry : activeEntries) {
                long allocatedSaving = proportionalSaving(requiredSaving, entry.currentSize(), totalActiveSize);
                long targetSize = Math.max(1L, entry.currentSize() - allocatedSaving);
                entry.setAssignedTargetSize(targetSize);
                CompressionAttempt attempt = compressEntry(entry, targetSize, workDirectory, round);
                entry.setTargetAchieved(entry.currentSize() <= targetSize);
                entry.setMessage(attemptMessage(attempt, entry.getTargetAchieved()));
                reduced |= attempt.reduced();
                if (!attempt.reduced() || !attempt.providerTargetReached() || entry.currentSize() <= 1) {
                    entry.setActive(false);
                }
            }
            if (!reduced) {
                break;
            }
            currentZipSize = writeZip(zipPath, entries);
            if (currentZipSize < best.zipSize()) {
                best = snapshot(zipPath, currentZipSize, entries, workDirectory);
            }
            round++;
        }
        return best;
    }

    private long proportionalSaving(long requiredSaving, long entrySize, long totalSize) {
        BigInteger numerator = BigInteger.valueOf(requiredSaving).multiply(BigInteger.valueOf(entrySize));
        BigInteger denominator = BigInteger.valueOf(Math.max(1L, totalSize));
        long allocated = numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator).longValue();
        return Math.min(Math.max(1L, allocated), Math.max(1L, entrySize - 1));
    }

    private CompressionAttempt compressEntry(EntryWork entry,
                                             long targetSize,
                                             Path workDirectory,
                                             int round) {
        long beforeSize = entry.currentSize();
        try (InputStream input = Files.newInputStream(entry.sourcePath())) {
            CompressFileResultVO compressed = entry.compressApi().compress(new CompressFileCommand(
                    entry.fileName(),
                    entry.contentType(),
                    input,
                    entry.compression(),
                    targetSize));
            byte[] content = compressed.content();
            if (content.length >= beforeSize) {
                return new CompressionAttempt(false, compressed.targetReached());
            }
            Path outputPath = workDirectory.resolve("entry-" + entry.index() + "-compressed-" + round);
            Files.write(outputPath, content);
            entry.setCurrentPath(outputPath);
            entry.setCurrentSize(content.length);
            entry.setCompressionApplied(true);
            return new CompressionAttempt(true, compressed.targetReached());
        } catch (IOException | RuntimeException ex) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private String attemptMessage(CompressionAttempt attempt, Boolean targetAchieved) {
        if (!attempt.reduced()) {
            return "压缩组件无法进一步缩小文件";
        }
        return Boolean.TRUE.equals(targetAchieved) ? "已压缩并达到当前目标" : "已压缩但未达到当前目标";
    }

    private long writeZip(Path zipPath, List<EntryWork> entries) {
        try (OutputStream output = Files.newOutputStream(zipPath);
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (EntryWork entry : entries) {
                zipOutput.putNextEntry(new ZipEntry(entry.path()));
                Files.copy(entry.currentPath(), zipOutput);
                zipOutput.closeEntry();
            }
            zipOutput.finish();
        } catch (IOException ex) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }
        return fileSize(zipPath);
    }

    private PackageSnapshot snapshot(Path zipPath,
                                     long zipSize,
                                     List<EntryWork> entries,
                                     Path workDirectory) {
        Path bestZipPath = workDirectory.resolve(BEST_ZIP_FILE_NAME);
        try {
            Files.copy(zipPath, bestZipPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }
        List<EntrySnapshot> entrySnapshots = entries.stream().map(EntrySnapshot::from).toList();
        return new PackageSnapshot(bestZipPath, zipSize, entrySnapshots);
    }

    private FilePackageResultVO result(FilePackageSizeControlCommand command, PackageSnapshot snapshot) {
        FilePackageResultVO result = new FilePackageResultVO();
        result.setSizeControlMode(command.getSizeControlMode());
        result.setMaxPackageSizeBytes(command.getMaxPackageSizeBytes());
        result.setActualPackageSizeBytes(snapshot.zipSize());
        if (command.getMaxPackageSizeBytes() != null) {
            result.setPackageTargetAchieved(snapshot.zipSize() <= command.getMaxPackageSizeBytes());
        }
        if (command.getSizeControlMode() == FilePackageSizeControlMode.MANUAL) {
            List<EntrySnapshot> targeted = snapshot.entries().stream()
                    .filter(entry -> entry.assignedTargetSize() != null)
                    .toList();
            if (!targeted.isEmpty()) {
                result.setEntryTargetsAchieved(targeted.stream()
                        .allMatch(entry -> Boolean.TRUE.equals(entry.targetAchieved())));
            }
        }
        result.setCompressionApplied(snapshot.entries().stream().anyMatch(EntrySnapshot::compressionApplied));
        result.setEntries(snapshot.entries().stream().map(this::entryResult).toList());
        result.setMessage(resultMessage(command.getSizeControlMode(), result));
        return result;
    }

    private FilePackageEntryResultVO entryResult(EntrySnapshot entry) {
        FilePackageEntryResultVO result = new FilePackageEntryResultVO();
        result.setFileId(entry.fileId());
        result.setPath(entry.path());
        result.setOriginalSizeBytes(entry.originalSize());
        result.setOutputSizeBytes(entry.currentSize());
        result.setTargetSizeBytes(entry.assignedTargetSize());
        result.setCompressionSupported(entry.compressionSupported());
        result.setCompressionApplied(entry.compressionApplied());
        result.setTargetAchieved(entry.targetAchieved());
        result.setMessage(entry.message());
        return result;
    }

    private String resultMessage(FilePackageSizeControlMode mode, FilePackageResultVO result) {
        if (mode == FilePackageSizeControlMode.AUTO) {
            if (Boolean.TRUE.equals(result.getPackageTargetAchieved())) {
                return String.format("最终ZIP已达到目标大小：目标%d字节，实际%d字节",
                        result.getMaxPackageSizeBytes(), result.getActualPackageSizeBytes());
            }
            return String.format("最终ZIP未达到目标大小：目标%d字节，当前只能压缩到%d字节；"
                            + "NONE及不支持压缩的文件保持原样，可压缩文件已按大小比例处理",
                    result.getMaxPackageSizeBytes(), result.getActualPackageSizeBytes());
        }
        List<String> messages = new ArrayList<>();
        messages.add("已按逐文件手动目标处理");
        if (Boolean.FALSE.equals(result.getEntryTargetsAchieved())) {
            messages.add("部分文件未达到目标大小");
        }
        if (Boolean.FALSE.equals(result.getPackageTargetAchieved())) {
            messages.add(String.format("最终ZIP可选总目标为%d字节，实际为%d字节，未执行自动补压缩",
                    result.getMaxPackageSizeBytes(), result.getActualPackageSizeBytes()));
        }
        return String.join("；", messages);
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Failed to delete temporary package path: {}", path, ex);
                }
            });
        } catch (IOException ex) {
            log.warn("Failed to enumerate temporary package directory: {}", directory, ex);
        }
    }

    @FunctionalInterface
    interface DownloadResolver extends Function<Long, FileDownloadVO> {
    }

    static final class PackageBuild implements AutoCloseable {

        private final Path workDirectory;
        private final Path zipPath;
        private final FilePackageResultVO result;

        private PackageBuild(Path workDirectory, Path zipPath, FilePackageResultVO result) {
            this.workDirectory = workDirectory;
            this.zipPath = zipPath;
            this.result = result;
        }

        Path zipPath() {
            return zipPath;
        }

        FilePackageResultVO result() {
            return result;
        }

        @Override
        public void close() {
            deleteRecursively(workDirectory);
        }
    }

    private record CompressionAttempt(boolean reduced, boolean providerTargetReached) {
    }

    private record PackageSnapshot(Path zipPath, long zipSize, List<EntrySnapshot> entries) {
    }

    private record EntrySnapshot(Long fileId,
                                 String path,
                                 long originalSize,
                                 long currentSize,
                                 Long assignedTargetSize,
                                 boolean compressionSupported,
                                 boolean compressionApplied,
                                 Boolean targetAchieved,
                                 String message) {

        private static EntrySnapshot from(EntryWork entry) {
            return new EntrySnapshot(entry.fileId(),
                    entry.path(),
                    entry.originalSize(),
                    entry.currentSize(),
                    entry.getAssignedTargetSize(),
                    entry.compressApi() != null,
                    entry.isCompressionApplied(),
                    entry.getTargetAchieved(),
                    entry.getMessage());
        }
    }

    private static final class EntryWork {

        private final int index;
        private final Long fileId;
        private final String path;
        private final String fileName;
        private final String contentType;
        private final Path sourcePath;
        private final long originalSize;
        private final FileCompression compression;
        private final FileCompressApi compressApi;
        private final Long requestedTargetSize;
        private Path currentPath;
        private long currentSize;
        private Long assignedTargetSize;
        private boolean compressionApplied;
        private Boolean targetAchieved;
        private String message;
        private boolean active;

        private EntryWork(int index,
                          Long fileId,
                          String path,
                          String fileName,
                          String contentType,
                          Path sourcePath,
                          long originalSize,
                          FileCompression compression,
                          FileCompressApi compressApi,
                          Long requestedTargetSize) {
            this.index = index;
            this.fileId = fileId;
            this.path = path;
            this.fileName = fileName;
            this.contentType = contentType;
            this.sourcePath = sourcePath;
            this.originalSize = originalSize;
            this.compression = compression;
            this.compressApi = compressApi;
            this.requestedTargetSize = requestedTargetSize;
            this.currentPath = sourcePath;
            this.currentSize = originalSize;
        }

        private int index() {
            return index;
        }

        private Long fileId() {
            return fileId;
        }

        private String path() {
            return path;
        }

        private String fileName() {
            return fileName;
        }

        private String contentType() {
            return contentType;
        }

        private Path sourcePath() {
            return sourcePath;
        }

        private long originalSize() {
            return originalSize;
        }

        private Path currentPath() {
            return currentPath;
        }

        private void setCurrentPath(Path currentPath) {
            this.currentPath = Objects.requireNonNull(currentPath);
        }

        private long currentSize() {
            return currentSize;
        }

        private void setCurrentSize(long currentSize) {
            this.currentSize = currentSize;
        }

        private FileCompression compression() {
            return compression;
        }

        private FileCompressApi compressApi() {
            return compressApi;
        }

        private Long requestedTargetSize() {
            return requestedTargetSize;
        }

        private Long getAssignedTargetSize() {
            return assignedTargetSize;
        }

        private void setAssignedTargetSize(Long assignedTargetSize) {
            this.assignedTargetSize = assignedTargetSize;
        }

        private boolean isCompressionApplied() {
            return compressionApplied;
        }

        private void setCompressionApplied(boolean compressionApplied) {
            this.compressionApplied = compressionApplied;
        }

        private Boolean getTargetAchieved() {
            return targetAchieved;
        }

        private void setTargetAchieved(Boolean targetAchieved) {
            this.targetAchieved = targetAchieved;
        }

        private String getMessage() {
            return message;
        }

        private void setMessage(String message) {
            this.message = message;
        }

        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }
    }
}
