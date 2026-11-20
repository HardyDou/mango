package io.mango.file.preview.support.remote;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import cn.keking.utils.DownloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 独立部署引擎的取源下载器：引擎收到带 {@code fileId} 的请求时走本实现，按票据回 Mango 直取源文件。
 *
 * <p>内嵌部署由 {@code io.mango.file.preview.starter.FilePreviewEngineDownloader} 在同进程内取流，
 * 本实现只在引擎进程独立部署、且 {@code mango.file-preview.remote.enabled=true} 时装配，
 * 从而保持「{@code fileId} 是主路径」，不再把票据换成对外 {@code url} 让 Mango 自己再抓一次。</p>
 */
public class FilePreviewRemoteSourceDownloader implements FileDownloader, InitializingBean, DisposableBean {

    /** 日志里保留的取源票据前缀长度。 */
    private static final int SOURCE_ID_VISIBLE_PREFIX = 6;

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRemoteSourceDownloader.class);

    private final FilePreviewSourceTransport transport;

    public FilePreviewRemoteSourceDownloader(FilePreviewSourceTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean supports(FileAttribute fileAttribute) {
        return fileAttribute != null && StringUtils.hasText(fileAttribute.getSourceId());
    }

    @Override
    public boolean verify(String sourceId) {
        if (!StringUtils.hasText(sourceId)) {
            return false;
        }
        try {
            return transport.isSourceTokenValid(sourceId);
        } catch (RuntimeException e) {
            LOGGER.warn("远程校验预览取源票据失败，按未确认处理，fileId：{}，原因：{}",
                    maskedSourceId(sourceId), e.getMessage());
            return false;
        }
    }

    @Override
    public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
        String realPath = DownloadUtils.getRelFilePath(fileName, fileAttribute);
        try {
            File target = new File(realPath);
            createParentDirectory(target);
            transport.downloadSource(fileAttribute.getSourceId(), target);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("远程读取预览源文件失败，fileId：{}", maskedSourceId(fileAttribute.getSourceId()), e);
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
    public void afterPropertiesSet() {
        FileDownloaders.register(this);
    }

    @Override
    public void destroy() {
        FileDownloaders.unregister(this);
    }

    private void createParentDirectory(File target) throws IOException {
        Path parent = target.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 取源票据是短期有效的凭证，日志里只保留前 6 位与长度，避免整串凭证落入日志系统。
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
