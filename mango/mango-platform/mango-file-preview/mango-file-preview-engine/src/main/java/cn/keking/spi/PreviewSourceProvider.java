package cn.keking.spi;

import java.io.IOException;
import java.io.InputStream;

/**
 * 预览源文件直取实现。
 *
 * <p>引擎默认用 HTTP 抓取 {@code url} 参数指向的源文件。内嵌在 Mango 进程里时，业务侧可以
 * 注册一个实现，让 {@code fileid://} 地址直接读本进程的文件能力，省掉一次 HTTP 往返、
 * 一次临时落盘的出口流量以及对对外域名的依赖。</p>
 *
 * <p>实现必须是线程安全的，并在使用期内保持可用；{@link #open(String)} 返回的流由调用方关闭。</p>
 */
public interface PreviewSourceProvider {

    /**
     * 打开源文件流。
     *
     * @param sourceId 源标识，来自 {@code fileid://<sourceId>}
     * @return 源文件流，调用方负责关闭
     * @throws IOException 源不存在或读取失败
     */
    InputStream open(String sourceId) throws IOException;

    /**
     * 实现名，仅用于日志与排障。
     *
     * @return 实现名
     */
    default String name() {
        String simpleName = getClass().getSimpleName();
        return simpleName.isBlank() ? getClass().getName() : simpleName;
    }
}
