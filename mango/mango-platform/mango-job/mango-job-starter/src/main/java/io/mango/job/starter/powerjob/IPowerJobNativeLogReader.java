package io.mango.job.starter.powerjob;

/**
 * PowerJob 原生日志读取器。
 */
public interface IPowerJobNativeLogReader {

    PowerJobNativeLog readInstanceLog(Long instanceId);
}
