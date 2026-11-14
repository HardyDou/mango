package io.mango.file.core.service.impl;

import io.mango.common.result.Require;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * 文件输入抽象：统一处理 multipart、命令流和临时文件三种来源。
 */
final class FileInput implements AutoCloseable {

    final Supplier<InputStream> streamSupplier;
    final Path tempPath;
    final String fileName;
    final long fileSize;
    final String contentType;
    final boolean empty;

    private FileInput(Supplier<InputStream> streamSupplier,
                      Path tempPath,
                      String fileName,
                      long fileSize,
                      String contentType,
                      boolean empty) {
        this.streamSupplier = streamSupplier;
        this.tempPath = tempPath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.empty = empty;
    }

    static FileInput fromMultipart(MultipartFile file) {
        return new FileInput(() -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }, null, file.getOriginalFilename(), file.getSize(), file.getContentType(), file.isEmpty());
    }

    static FileInput fromCommand(SaveFileCommand command) throws IOException {
        Require.notNull(command.getInputStream(), FileCode.FILE_EMPTY);
        Path temp = Files.createTempFile("mango-file-", ".upload");
        long size;
        try (InputStream input = command.getInputStream(); OutputStream output = Files.newOutputStream(temp)) {
            size = input.transferTo(output);
        }
        long declaredSize = size;
        if (command.getFileSize() != null && command.getFileSize() > 0) {
            declaredSize = command.getFileSize();
        }
        return new FileInput(() -> {
            try {
                return Files.newInputStream(temp);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }, temp, command.getFileName(), declaredSize, command.getContentType(), declaredSize <= 0);
    }

    static FileInput fromPath(Path path, String fileName, String contentType) throws IOException {
        long size = Files.size(path);
        return new FileInput(() -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }, null, fileName, size, contentType, size <= 0);
    }

    InputStream openStream() {
        return streamSupplier.get();
    }

    @Override
    public void close() throws IOException {
        if (tempPath != null) {
            Files.deleteIfExists(tempPath);
        }
    }
}
