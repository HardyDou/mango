package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CompressFileReaderTest {

    private final String originalFileDir = ConfigConstants.getFileDir();

    @TempDir
    Path tempDir;

    @AfterEach
    void restoreFileDir() {
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void unRar_whenSevenZipArchive_extractsEntriesWithoutNativeLibrary() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        writeSevenZArchive("docs/demo.txt", "seven zip content");

        CompressFileReader reader = new CompressFileReader(mock(FileHandlerService.class));
        String folder = reader.unRar(tempDir.resolve("bundle.7z").toString(), null, "bundle.7z",
                new FileAttribute(FileType.COMPRESS, "7z", "bundle.7z", null));

        assertThat(Files.readString(tempDir.resolve(folder).resolve("docs/demo.txt")))
                .isEqualTo("seven zip content");
    }

    @Test
    void unRar_whenSevenZipEntryTraversesOutsideFolder_rejectsExtraction() throws Exception {
        ConfigConstants.setFileDirValue(tempDir.toString());
        writeSevenZArchive("../escape.txt", "unsafe");

        CompressFileReader reader = new CompressFileReader(mock(FileHandlerService.class));

        assertThatThrownBy(() -> reader.unRar(tempDir.resolve("bundle.7z").toString(), null, "bundle.7z",
                new FileAttribute(FileType.COMPRESS, "7z", "bundle.7z", null)))
                .hasRootCauseInstanceOf(SecurityException.class);
        assertThat(Files.exists(tempDir.resolve("escape.txt"))).isFalse();
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
