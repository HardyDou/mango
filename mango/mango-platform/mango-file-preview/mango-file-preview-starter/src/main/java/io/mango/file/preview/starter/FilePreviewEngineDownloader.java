package io.mango.file.preview.starter;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import cn.keking.utils.DownloadUtils;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.PreviewSourceAvailability;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 内嵌引擎的取源下载器：引擎收到带 {@code fileId} 的请求时走本实现，在同进程内读取源文件，
 * 不再绕对外域名做一次 HTTP 抓取。
 *
 * <p>同时实现 {@link PreviewSourceAvailability}，让 core 侧知道当前进程可以直接取流，从而在
 * 组装引擎预览地址时选择 {@code fileId} 参数而不是 {@code url} 参数。</p>
 */
public class FilePreviewEngineDownloader implements FileDownloader, PreviewSourceAvailability,
        InitializingBean, DisposableBean {

    /** 日志里保留的取源票据前缀长度。 */
    private static final int SOURCE_ID_VISIBLE_PREFIX = 6;

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewEngineDownloader.class);

    private final IFilePreviewService filePreviewService;

    public FilePreviewEngineDownloader(IFilePreviewService filePreviewService) {
        this.filePreviewService = filePreviewService;
    }

    @Override
    public boolean supports(FileAttribute fileAttribute) {
        return fileAttribute != null && StringUtils.hasText(fileAttribute.getSourceId());
    }

    @Override
    public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
        String realPath = DownloadUtils.getRelFilePath(fileName, fileAttribute);
        try (InputStream inputStream = openSource(fileAttribute.getSourceId())) {
            FileUtils.copyInputStreamToFile(inputStream, new File(realPath));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("进程内读取预览源文件失败，fileId：{}", maskedSourceId(fileAttribute.getSourceId()), e);
            response.setCode(1);
            response.setContent(null);
            response.setMsg("下载失败: " + e.getMessage());
            return response;
        }
        response.setContent(realPath);
        response.setMsg(fileName);
        return response;
    }

    @Override
    public boolean internalSourceAvailable() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {
        FileDownloaders.register(this);
    }

    @Override
    public void destroy() {
        FileDownloaders.unregister(this);
    }

    private InputStream openSource(String sourceId) {
        FilePreviewSource source = filePreviewService.openSource(sourceId);
        return source.inputStream();
    }

    /**
     * 取源票据是 24 小时有效的凭证，日志里只保留前 6 位与长度，避免整串凭证落入日志系统。
     */
    private static String maskedSourceId(String sourceId) {
        if (!StringUtils.hasText(sourceId)) {
            return "";
        }
        String prefix = sourceId.length() <= SOURCE_ID_VISIBLE_PREFIX
                ? sourceId
                : sourceId.substring(0, SOURCE_ID_VISIBLE_PREFIX);
        return prefix + "***(len=" + sourceId.length() + ")";
    }
}
