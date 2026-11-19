package cn.keking.spi;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link FileDownloader} 注册表：按注册顺序取第一个 {@code supports} 的实现，没有则由引擎内置下载兜底。
 *
 * <p>只允许在进程启动阶段注册；注册表是全局静态的，供 {@code DownloadUtils} 这类静态工具调用。</p>
 */
public final class FileDownloaders {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloaders.class);

    private static final List<FileDownloader> DOWNLOADERS = new CopyOnWriteArrayList<>();

    private FileDownloaders() {
    }

    public static void register(FileDownloader downloader) {
        if (downloader == null) {
            return;
        }
        DOWNLOADERS.add(downloader);
        LOGGER.info("注册文件下载器: {}", downloader.getClass().getName());
    }

    public static void unregister(FileDownloader downloader) {
        if (downloader != null && DOWNLOADERS.remove(downloader)) {
            LOGGER.info("注销文件下载器: {}", downloader.getClass().getName());
        }
    }

    /**
     * 尝试用已注册的下载器处理。
     *
     * @return 没有下载器支持时返回 {@code null}，交由引擎内置实现处理
     */
    public static ReturnResponse<String> tryDownload(FileAttribute fileAttribute, String fileName) {
        if (fileAttribute == null) {
            return null;
        }
        for (FileDownloader downloader : DOWNLOADERS) {
            if (downloader.supports(fileAttribute)) {
                return downloader.download(fileAttribute, fileName);
            }
        }
        return null;
    }
}
