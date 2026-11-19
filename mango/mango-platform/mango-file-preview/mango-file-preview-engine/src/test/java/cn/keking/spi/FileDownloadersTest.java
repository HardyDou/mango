package cn.keking.spi;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileDownloadersTest {

    private FileDownloader downloader;

    @AfterEach
    void unregister() {
        FileDownloaders.unregister(downloader);
        downloader = null;
    }

    @Test
    void tryDownload_命中支持该属性的下载器() {
        downloader = new StubDownloader(true, "done");
        FileDownloaders.register(downloader);
        FileAttribute attribute = attributeWithSourceId("source-1");

        ReturnResponse<String> response = FileDownloaders.tryDownload(attribute, "demo.txt");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("done");
    }

    @Test
    void tryDownload_没有下载器支持时返回空交由内置实现() {
        downloader = new StubDownloader(false, "done");
        FileDownloaders.register(downloader);

        assertThat(FileDownloaders.tryDownload(attributeWithSourceId("source-1"), "demo.txt")).isNull();
    }

    @Test
    void tryDownload_属性为空时返回空() {
        assertThat(FileDownloaders.tryDownload(null, "demo.txt")).isNull();
    }

    @Test
    void unregister_之后不再参与分发() {
        downloader = new StubDownloader(true, "done");
        FileDownloaders.register(downloader);
        FileDownloaders.unregister(downloader);

        assertThat(FileDownloaders.tryDownload(attributeWithSourceId("source-1"), "demo.txt")).isNull();
    }

    @Test
    void register_忽略空实现() {
        FileDownloaders.register(null);

        assertThat(FileDownloaders.tryDownload(attributeWithSourceId("source-1"), "demo.txt")).isNull();
    }

    @Test
    void tryDownload_按注册顺序取第一个匹配实现() {
        downloader = new StubDownloader(true, "first");
        FileDownloaders.register(downloader);
        FileDownloader second = new StubDownloader(true, "second");
        try {
            FileDownloaders.register(second);

            List<String> result = List.of(FileDownloaders.tryDownload(attributeWithSourceId("s"), "d.txt").getContent());

            assertThat(result).containsExactly("first");
        } finally {
            FileDownloaders.unregister(second);
        }
    }

    private FileAttribute attributeWithSourceId(String sourceId) {
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", null);
        attribute.setSourceId(sourceId);
        return attribute;
    }

    private static final class StubDownloader implements FileDownloader {

        private final boolean supports;
        private final String content;

        private StubDownloader(boolean supports, String content) {
            this.supports = supports;
            this.content = content;
        }

        @Override
        public boolean supports(FileAttribute fileAttribute) {
            return supports;
        }

        @Override
        public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
            return new ReturnResponse<>(0, "ok", content);
        }
    }
}
