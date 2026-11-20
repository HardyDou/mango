package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.cache.impl.CacheServiceJDKImpl;
import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fileId 直取通道（票据取源）下，图片页里的子资源地址必须落在 {@code /preview-file} 前缀上。
 *
 * <p>浏览器打开预览页时不带 {@code Authorization} 头，票据地址在浏览器侧没有对应 handler，
 * 因此页面里只能出现引擎落盘文件的对外地址。</p>
 */
class PictureFilePreviewImplTest {

    @TempDir
    Path fileDir;

    private String originalFileDir;
    private Boolean originalCacheEnabled;
    private FileDownloader downloader;
    private PictureFilePreviewImpl preview;

    @BeforeEach
    void setUp() {
        originalFileDir = ConfigConstants.getFileDir();
        originalCacheEnabled = ConfigConstants.isCacheEnabled();
        ConfigConstants.setFileDirValue(fileDir.toString());
        ConfigConstants.setCacheEnabledValueValue(Boolean.TRUE);
        CacheServiceJDKImpl cache = new CacheServiceJDKImpl();
        cache.initCache();
        preview = new PictureFilePreviewImpl(new FileHandlerService(cache), new OtherFilePreviewImpl());
        downloader = new SourceTicketDownloader(fileDir);
        FileDownloaders.register(downloader);
    }

    @AfterEach
    void tearDown() {
        FileDownloaders.unregister(downloader);
        ConfigConstants.setCacheEnabledValueValue(originalCacheEnabled);
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void ticketSourceIsRenderedWithPublicFilePrefix() throws IOException {
        Model model = new ExtendedModelMap();

        String view = preview.filePreviewHandle("8f14e45fceea167a5a36dedd4bea2543", model, picture("demo.png", "8f14e45fceea167a5a36dedd4bea2543"));

        assertThat(view).isEqualTo(FilePreview.PICTURE_FILE_PREVIEW_PAGE);
        assertThat(model.getAttribute("imgUrls")).isEqualTo("/preview-file/demo.png");
        // 引擎内部相对路径不含前导斜杠，对外地址由 toPublicPath 统一补全。
        assertThat(model.getAttribute("currentUrl")).isEqualTo("demo.png");
        assertThat(Files.isRegularFile(fileDir.resolve("demo.png"))).isTrue();
    }

    @Test
    void pageNeverCarriesTheSourceTicketAsImageAddress() {
        Model model = new ExtendedModelMap();

        preview.filePreviewHandle("8f14e45fceea167a5a36dedd4bea2543", model, picture("demo.png", "8f14e45fceea167a5a36dedd4bea2543"));

        assertThat(String.valueOf(model.getAttribute("imgUrls"))).doesNotContain("8f14e45fceea167a5a36dedd4bea2543");
    }

    @Test
    void httpSourceKeepsItsOwnAddress() {
        Model model = new ExtendedModelMap();

        preview.filePreviewHandle("http://files.example.com/a.png", model, picture("a.png", null));

        assertThat(model.getAttribute("imgUrls")).isEqualTo("http://files.example.com/a.png");
    }

    private static FileAttribute picture(String name, String sourceId) {
        FileAttribute fileAttribute = new FileAttribute();
        fileAttribute.setName(name);
        fileAttribute.setSuffix(name.substring(name.lastIndexOf('.') + 1));
        fileAttribute.setType(FileType.PICTURE);
        fileAttribute.setSourceId(sourceId);
        return fileAttribute;
    }

    /**
     * 取源票据的落盘实现：只处理带票据的取源请求，落盘位置与平台下载器一致（引擎 file 目录 + 文件名）。
     */
    private record SourceTicketDownloader(Path fileDir) implements FileDownloader {

        @Override
        public boolean supports(FileAttribute fileAttribute) {
            return fileAttribute.getSourceId() != null;
        }

        @Override
        public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
            Path target = fileDir.resolve(fileName);
            try {
                Files.writeString(target, "png-bytes", StandardCharsets.UTF_8);
            } catch (IOException e) {
                return new ReturnResponse<>(ReturnResponse.FAILURE_CODE, e.getMessage(), null);
            }
            return new ReturnResponse<>(ReturnResponse.SUCCESS_CODE, ReturnResponse.SUCCESS_MSG, target.toString());
        }
    }
}
