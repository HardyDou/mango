package io.mango.file.preview.starter;

import cn.keking.spi.PreviewSourceProvider;
import cn.keking.spi.PreviewSourceProviders;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.PreviewSourceAvailability;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.InputStream;

/**
 * 内嵌引擎的源文件直取适配器。
 *
 * <p>把 {@link IFilePreviewService#openSource(String)} 暴露给引擎的 {@code fileid://} 分支，
 * 同一进程内直接读文件能力，不再让引擎通过对外域名 HTTP 抓回。实现随容器启动注册、销毁时注销，
 * 同时充当 {@link PreviewSourceAvailability}，让 {@code auto} 模式能判断当前进程是否可直取。</p>
 */
public class FilePreviewEngineSourceProvider implements PreviewSourceProvider, PreviewSourceAvailability,
        InitializingBean, DisposableBean {

    private final IFilePreviewService filePreviewService;

    public FilePreviewEngineSourceProvider(IFilePreviewService filePreviewService) {
        this.filePreviewService = filePreviewService;
    }

    @Override
    public InputStream open(String sourceId) throws IOException {
        try {
            FilePreviewSource source = filePreviewService.openSource(sourceId);
            return source.inputStream();
        } catch (RuntimeException e) {
            throw new IOException("读取预览源文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean internalSourceAvailable() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {
        PreviewSourceProviders.register(this);
    }

    @Override
    public void destroy() {
        PreviewSourceProviders.unregister(this);
    }

    @Override
    public String name() {
        return "mango-file-preview";
    }
}
