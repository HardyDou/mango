package io.mango.infra.docsign.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * File-backed document source used when a format requires random access.
 */
final class TemporaryDocumentFile implements AutoCloseable {

    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final Path path;

    private TemporaryDocumentFile(Path path) {
        this.path = path;
    }

    static TemporaryDocumentFile copyOf(InputStream source, Path directory, long maxBytes) throws IOException {
        Files.createDirectories(directory);
        Path file = Files.createTempFile(directory, "mango-docsign-", ".document");
        try {
            restrictPermissions(file);
            try (OutputStream output = Files.newOutputStream(file)) {
                copy(source, output, maxBytes);
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(file);
            throw ex;
        }
        return new TemporaryDocumentFile(file);
    }

    Path path() {
        return path;
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }

    private static void restrictPermissions(Path file) throws IOException {
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        }
    }

    private static void copy(InputStream source, OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[65536];
        long copied = 0;
        int read;
        while ((read = source.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            copied += read;
            if (copied > maxBytes) {
                throw new IllegalArgumentException(
                        "文档超过流式处理大小上限 " + maxBytes + " 字节");
            }
            output.write(buffer, 0, read);
        }
    }
}
