package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import cn.keking.service.cache.impl.CacheServiceJDKImpl;
import cn.keking.service.impl.OtherFilePreviewImpl;
import cn.keking.service.impl.SimTextFilePreviewImpl;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实链路验证：浏览器地址里只有 {@code fileId}，引擎在同一个 JVM 内取到源文件，
 * 不依赖对外域名、不依赖 {@code trust.host} 白名单。
 */
class FilePreviewFileIdRequestFlowTest {

    @TempDir
    Path tempDir;

    private String originalFileDir;

    private Boolean originalCacheEnabled;



    private IFilePreviewService filePreviewService;

    private FilePreviewEngineDownloader downloader;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        originalFileDir = ConfigConstants.getFileDir();
        originalCacheEnabled = ConfigConstants.isCacheEnabled();
        ConfigConstants.setFileDirValue(tempDir.toString());
        ConfigConstants.setCacheEnabledValueValue(false);
        ConfigConstants.setTrustHostValue("default");
        ConfigConstants.setKeyValue("false");

        filePreviewService = mock(IFilePreviewService.class);
        downloader = new FilePreviewEngineDownloader(filePreviewService);
        downloader.afterPropertiesSet();

        CacheServiceJDKImpl cacheService = new CacheServiceJDKImpl();
        cacheService.initCache();
        FileHandlerService handlerService = new FileHandlerService(cacheService);
        ReflectionTestUtils.setField(handlerService, "uriEncoding", "UTF-8");
        OtherFilePreviewImpl otherFilePreview = new OtherFilePreviewImpl();
        SimTextFilePreviewImpl textPreview = new SimTextFilePreviewImpl(handlerService, otherFilePreview);
        FilePreviewFactory factory = mock(FilePreviewFactory.class);
        when(factory.get(org.mockito.ArgumentMatchers.any())).thenReturn((FilePreview) textPreview);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new cn.keking.web.controller.OnlinePreviewController(
                        factory, handlerService, new CacheServiceJDKImpl(), otherFilePreview)).build();
    }

    @AfterEach
    void tearDown() {
        downloader.destroy();
        ConfigConstants.setFileDirValue(originalFileDir);
        ConfigConstants.setCacheEnabledValueValue(originalCacheEnabled);
        ConfigConstants.setTrustHostValue("default");
        ConfigConstants.setKeyValue("false");
    }

    @Test
    void onlinePreview_fileId在信任列表留空时也能读到源文件() throws Exception {
        assertThat(ConfigConstants.getTrustHostSet()).isEmpty();
        when(filePreviewService.openSource("ticket-1")).thenReturn(new FilePreviewSource(
                new ByteArrayInputStream("hello from fileId".getBytes(StandardCharsets.UTF_8)),
                "demo.txt", "text/plain", 17L));

        var result = mockMvc.perform(get("/onlinePreview")
                        .param("fileId", "ticket-1")
                        .param("fullfilename", "demo.txt"))
                .andExpect(status().isOk())
                .andReturn();

        String textData = (String) result.getModelAndView().getModel().get("textData");
        assertThat(textData).isNotNull();
        assertThat(new String(java.util.Base64.getDecoder().decode(textData), StandardCharsets.UTF_8))
                .contains("hello from fileId");
    }

    @Test
    void onlinePreview_伪造或过期票据不会读到任何内容() throws Exception {
        when(filePreviewService.openSource("forged-ticket"))
                .thenThrow(new IllegalStateException("预览令牌无效或已过期"));

        var result = mockMvc.perform(get("/onlinePreview")
                        .param("fileId", "forged-ticket")
                        .param("fullfilename", "demo.txt"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getModelAndView().getModel()).containsKey("msg");
        assertThat(String.valueOf(result.getModelAndView().getModel().get("textData")))
                .doesNotContain("hello from fileId");
        assertThat(storedContents()).noneMatch(content -> content.contains("hello from fileId"));
    }

    private java.util.List<String> storedContents() throws java.io.IOException {
        try (var files = java.nio.file.Files.walk(tempDir)) {
            return files.filter(java.nio.file.Files::isRegularFile)
                    .map(path -> {
                        try {
                            return java.nio.file.Files.readString(path);
                        } catch (java.io.IOException e) {
                            return "";
                        }
                    })
                    .toList();
        }
    }
}
