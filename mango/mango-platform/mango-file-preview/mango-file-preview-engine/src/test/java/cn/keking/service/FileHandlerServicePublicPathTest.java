package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.service.cache.impl.CacheServiceJDKImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 落盘文件对外地址的契约：引擎只对外暴露一个可枚举前缀，页面里的子资源不再指向根级文件路径。
 */
class FileHandlerServicePublicPathTest {

    @TempDir
    Path fileDir;

    private String originalFileDir;

    @BeforeEach
    void setUp() {
        originalFileDir = ConfigConstants.getFileDir();
        ConfigConstants.setFileDirValue(fileDir.toString());
    }

    @AfterEach
    void tearDown() {
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void declaresSinglePublicFilePrefix() {
        assertThat(FileHandlerService.PUBLIC_FILE_PATH_PREFIX).isEqualTo("/preview-file");
    }

    @Test
    void mapsAbsoluteEngineFileToPublicPrefix() {
        CacheServiceJDKImpl cache = new CacheServiceJDKImpl();
        cache.initCache();
        FileHandlerService fileHandlerService = new FileHandlerService(cache);

        String publicPath = fileHandlerService.getPublicPath(ConfigConstants.getFileDir() + "file-1-abc.mp4");

        assertThat(publicPath).isEqualTo("/preview-file/file-1-abc.mp4");
        assertThat(publicPath).doesNotContain(fileDir.toString());
    }

    @Test
    void keepsExactlyOneSlashBetweenPrefixAndRelativePath() {
        assertThat(FileHandlerService.toPublicPath("/file-1-abc.png")).isEqualTo("/preview-file/file-1-abc.png");
        assertThat(FileHandlerService.toPublicPath("file-1-abc.png")).isEqualTo("/preview-file/file-1-abc.png");
    }

    @Test
    void degradesToPrefixWhenRelativePathIsMissing() {
        assertThat(FileHandlerService.toPublicPath(null)).isEqualTo("/preview-file");
        assertThat(FileHandlerService.toPublicPath("")).isEqualTo("/preview-file");
    }
}
