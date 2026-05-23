package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeUtils;
import org.jodconverter.local.office.LocalOfficeManager;

import java.io.File;

/**
 * 本地 Office 进程管理器。
 */
public class OfficeManagerHolder implements AutoCloseable {

    private final File officeHome;

    private final int[] portNumbers;

    private final long taskExecutionTimeout;

    private LocalOfficeManager officeManager;

    public OfficeManagerHolder(File officeHome, int[] portNumbers, long taskExecutionTimeout) {
        Require.notNull(portNumbers, "Office 端口不能为空");
        this.officeHome = officeHome;
        this.portNumbers = portNumbers.clone();
        this.taskExecutionTimeout = taskExecutionTimeout;
    }

    /**
     * 启动本地 Office 转换进程。
     */
    public synchronized void start() {
        if (officeManager != null && officeManager.isRunning()) {
            return;
        }
        try {
            if (officeManager == null) {
                officeManager = buildOfficeManager();
            }
            officeManager.start();
        } catch (OfficeException ex) {
            throw new ConvertToolException("启动 Office 转换进程失败", ex);
        }
    }

    LocalOfficeManager officeManager() {
        start();
        return officeManager;
    }

    @Override
    public synchronized void close() {
        if (officeManager != null && officeManager.isRunning()) {
            OfficeUtils.stopQuietly(officeManager);
        }
    }

    private LocalOfficeManager buildOfficeManager() {
        File resolvedOfficeHome = officeHome == null ? LocalOfficeHomeResolver.resolve() : officeHome;
        if (resolvedOfficeHome == null) {
            throw new ConvertToolException("未找到 LibreOffice/OpenOffice 安装目录，请配置 mango.fileproc.convert.office-home");
        }
        return LocalOfficeManager.builder()
                .officeHome(resolvedOfficeHome)
                .portNumbers(portNumbers)
                .taskExecutionTimeout(taskExecutionTimeout)
                .build();
    }
}
