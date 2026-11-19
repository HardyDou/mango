package cn.keking.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link PreviewSourceProvider} 的进程级注册表。
 *
 * <p>引擎内部是静态工具调用，拿不到 Spring 容器，因此用静态注册表把容器里的实现暴露给
 * {@link cn.keking.utils.DownloadUtils}。同一进程只允许一个实现，后注册的替换先注册的。</p>
 */
public final class PreviewSourceProviders {

    private static final Logger logger = LoggerFactory.getLogger(PreviewSourceProviders.class);

    private static volatile PreviewSourceProvider provider;

    private PreviewSourceProviders() {
    }

    /**
     * 注册实现。
     *
     * @param sourceProvider 直取实现
     */
    public static void register(PreviewSourceProvider sourceProvider) {
        if (sourceProvider == null) {
            return;
        }
        PreviewSourceProvider previous = provider;
        provider = sourceProvider;
        if (previous != null && previous != sourceProvider) {
            logger.warn("预览源文件直取实现被替换: {} -> {}", previous.name(), sourceProvider.name());
        }
        logger.info("已注册预览源文件直取实现: {}", sourceProvider.name());
    }

    /**
     * 注销实现，仅当传入的仍是当前实现时生效。
     *
     * @param sourceProvider 直取实现
     */
    public static void unregister(PreviewSourceProvider sourceProvider) {
        if (sourceProvider != null && provider == sourceProvider) {
            provider = null;
        }
    }

    /**
     * 当前进程是否具备直取能力。
     *
     * @return 已注册实现时返回 true
     */
    public static boolean hasProvider() {
        return provider != null;
    }

    /**
     * 打开源文件流。
     *
     * @param sourceId 源标识
     * @return 源文件流，调用方负责关闭
     * @throws IOException 没有实现或读取失败
     */
    public static InputStream open(String sourceId) throws IOException {
        PreviewSourceProvider current = provider;
        if (current == null) {
            throw new IOException("当前进程没有注册 fileid:// 直取实现，无法读取源文件: " + sourceId);
        }
        return current.open(sourceId);
    }
}
