package io.mango.file.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileApiTest {

    @TempDir
    private Path tempDir;

    @Test
    void downloadTo_单文件下载到指定目录_返回落盘路径() throws Exception {
        FileApi fileApi = new StubFileApi();

        Path target = fileApi.downloadTo(1L, tempDir);

        assertThat(target).startsWith(tempDir);
        assertThat(target.getFileName().toString()).isEqualTo("demo.txt");
        assertThat(Files.readString(target)).isEqualTo("file-1");
    }

    @Test
    void downloadTo_批量下载到指定目录_返回文件路径映射() throws Exception {
        FileApi fileApi = new StubFileApi();

        Map<Long, Path> targets = fileApi.downloadTo(List.of(1L, 2L), tempDir);

        assertThat(targets).containsKeys(1L, 2L);
        assertThat(Files.readString(targets.get(1L))).isEqualTo("file-1");
        assertThat(Files.readString(targets.get(2L))).isEqualTo("file-2");
    }

    private static class StubFileApi implements FileApi {

        @Override
        public R<FileRecordVO> save(SaveFileCommand command) {
            throw unsupported();
        }

        @Override
        public R<PageResult<FileRecordVO>> page(FileRecordPageQuery query) {
            throw unsupported();
        }

        @Override
        public R<FileRecordVO> get(Long id) {
            throw unsupported();
        }

        @Override
        public R<FilePreviewVO> preview(Long id) {
            throw unsupported();
        }

        @Override
        public FileDownloadVO download(Long id) {
            byte[] content = ("file-" + id).getBytes(StandardCharsets.UTF_8);
            return new FileDownloadVO(new ByteArrayInputStream(content), "../demo.txt", "text/plain", content.length);
        }

        @Override
        public R<Boolean> archive(FileArchiveCommand command) {
            throw unsupported();
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not used by FileApi download tests");
        }
    }
}
