package io.mango.file.preview.core.service;

/**
 * 进程内是否具备预览源文件直取能力。
 *
 * <p>内嵌预览引擎的进程会注册实现（见 starter 的直取适配器），独立部署引擎的进程没有实现。
 * core 用它决定 {@code AUTO} 模式下走 {@code fileid://} 还是 HTTP。</p>
 */
@FunctionalInterface
public interface PreviewSourceAvailability {

    /**
     * 当前进程能否直接读取源文件。
     *
     * @return 可直取时返回 true
     */
    boolean internalSourceAvailable();
}
