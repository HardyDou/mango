package cn.keking.service;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import cn.keking.utils.DownloadUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验引擎默认下载入口可被外部下载器接管：带 sourceId 的属性不再走网络下载。
 */
class FileHandlerServiceSourceIdTest {

    private FileDownloader downloader;

    @AfterEach
    void unregister() {
        FileDownloaders.unregister(downloader);
        downloader = null;
    }

    @Test
    void downLoad_带源标识时交给注册的下载器且不访问网络() {
        downloader = new FileDownloader() {
            @Override
            public boolean supports(FileAttribute fileAttribute) {
                return fileAttribute.getSourceId() != null;
            }

            @Override
            public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
                return new ReturnResponse<>(0, fileName, "/tmp/" + fileAttribute.getSourceId());
            }
        };
        FileDownloaders.register(downloader);
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", null);
        attribute.setSourceId("source-1");

        ReturnResponse<String> response = DownloadUtils.downLoad(attribute, "demo.txt");

        assertThat(response.getCode()).isZero();
        assertThat(response.getContent()).isEqualTo("/tmp/source-1");
    }
}
