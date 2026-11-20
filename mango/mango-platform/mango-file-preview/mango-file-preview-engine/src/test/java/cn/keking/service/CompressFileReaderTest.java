package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompressFileReaderTest {

    private static final byte[] PNG_BYTES = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/AL+2wUAAAAASUVORK5CYII=");

    private final String originalFileDir = ConfigConstants.getFileDir();

    @TempDir
    Path tempDir;

    @AfterEach
    void restoreFileDir() {
        ConfigConstants.setFileDirValue(originalFileDir);
        ConfigConstants.setDecompressDirValue(ConfigConstants.DEFAULT_VALUE);
    }

    @Test
    void unRar_whenSevenZipArchive_extractsEntriesOutsideFileDir() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        writeSevenZArchive("docs/demo.txt", "seven zip content");

        CompressFileReader reader = new CompressFileReader(mock(FileHandlerService.class));
        String folder = reader.unRar(tempDir.resolve("bundle.7z").toString(), null, "bundle.7z",
                new FileAttribute(FileType.COMPRESS, "7z", "bundle.7z", null));

        Path decompressDir = tempDir.getParent().resolve(ConfigConstants.DEFAULT_DECOMPRESS_DIR_NAME);
        assertThat(ConfigConstants.getDecompressDir()).isEqualTo(decompressDir + File.separator);
        assertThat(Files.readString(decompressDir.resolve(folder).resolve("docs/demo.txt")))
                .isEqualTo("seven zip content");
        assertThat(Files.exists(tempDir.resolve(folder))).isFalse();
    }

    @Test
    void unRar_whenDecompressDirConfigured_extractsUnderConfiguredRoot() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        Path decompressDir = tempDir.resolve("extract-root");
        ConfigConstants.setDecompressDirValue(decompressDir.toString());
        writeSevenZArchive("docs/demo.txt", "configured content");

        CompressFileReader reader = new CompressFileReader(mock(FileHandlerService.class));
        String folder = reader.unRar(tempDir.resolve("bundle.7z").toString(), null, "bundle.7z",
                new FileAttribute(FileType.COMPRESS, "7z", "bundle.7z", null));

        assertThat(folder).isEqualTo("bundle.7z_");
        assertThat(Files.readString(decompressDir.resolve(folder).resolve("docs/demo.txt")))
                .isEqualTo("configured content");
    }

    @Test
    void unRar_whenSevenZipEntryTraversesOutsideFolder_rejectsExtraction() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        writeSevenZArchive("../escape.txt", "unsafe");

        CompressFileReader reader = new CompressFileReader(mock(FileHandlerService.class));

        assertThatThrownBy(() -> reader.unRar(tempDir.resolve("bundle.7z").toString(), null, "bundle.7z",
                new FileAttribute(FileType.COMPRESS, "7z", "bundle.7z", null)))
                .hasRootCauseInstanceOf(SecurityException.class);
        assertThat(Files.exists(tempDir.getParent().resolve(ConfigConstants.DEFAULT_DECOMPRESS_DIR_NAME)
                .resolve("escape.txt"))).isFalse();
    }

    @Test
    void unRar_whenNestedArchiveUnderDecompressDir_buildsRelativeCompressFileKey() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.resolve("file-dir").toString());
        Path decompressDir = tempDir.resolve("_extracted");
        Files.createDirectories(decompressDir.resolve("bundle.zip_"));
        ConfigConstants.setDecompressDirValue(decompressDir.toString());
        Path innerArchive = decompressDir.resolve("bundle.zip_").resolve("inner.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(innerArchive))) {
            zipOutputStream.putNextEntry(new ZipEntry("nested/readme.txt"));
            zipOutputStream.write("nested".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        FileAttribute nestedAttribute = new FileAttribute(FileType.COMPRESS, "zip", "inner.zip", null);
        nestedAttribute.setCompressFile(true);

        String key = new CompressFileReader(mock(FileHandlerService.class))
                .unRar(innerArchive.toString(), null, "inner.zip", nestedAttribute);

        assertThat(key).asString()
                .doesNotStartWith("/")
                .doesNotContain("_extracted")
                .doesNotContain("file-dir")
                .startsWith("_decompression")
                .endsWith("inner.zip_");
        assertThat(decompressDir.resolve(key)).as("解包目录应位于解包根下且层级唯一").isDirectory();
    }

    void unRar_whenArchiveContainsPicture_cachesCompressedFileEndpointUrl() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        writeZipArchive("pics/demo.png", PNG_BYTES);
        FileHandlerService fileHandlerService = mock(FileHandlerService.class);

        new CompressFileReader(fileHandlerService).unRar(tempDir.resolve("bundle.zip").toString(), null, "bundle.zip",
                new FileAttribute(FileType.COMPRESS, "zip", "bundle.zip", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> imgUrls = ArgumentCaptor.forClass(List.class);
        verify(fileHandlerService).putImgCache(eq("bundle.zip_"), imgUrls.capture());
        assertThat(imgUrls.getValue()).singleElement().asString()
                .contains("compressed-file?kkCompressfileKey=bundle.zip_")
                .contains("kkCompressfilepath=bundle.zip_%2Fpics%2Fdemo.png")
                .doesNotContain("/preview-file/");
    }

    private void writeZipArchive(String entryName, byte[] content) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempDir.resolve("bundle.zip")), StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content);
            zipOutputStream.closeEntry();
        }
    }

    private void writeSevenZArchive(String entryName, String content) throws IOException {
        Path source = Files.writeString(tempDir.resolve("source.txt"), content);
        try (SevenZOutputFile archive = new SevenZOutputFile(tempDir.resolve("bundle.7z").toFile())) {
            SevenZArchiveEntry entry = archive.createArchiveEntry(source.toFile(), entryName);
            archive.putArchiveEntry(entry);
            archive.write(Files.readAllBytes(source));
            archive.closeArchiveEntry();
        }
    }
}
