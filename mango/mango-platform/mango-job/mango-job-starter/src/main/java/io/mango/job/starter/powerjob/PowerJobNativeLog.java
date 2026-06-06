package io.mango.job.starter.powerjob;

import lombok.Getter;
import lombok.Setter;

/**
 * PowerJob 原生实例日志。
 */
@Getter
@Setter
public class PowerJobNativeLog {

    private boolean available;

    private String content;

    private String errorSummary;

    public static PowerJobNativeLog available(String content) {
        PowerJobNativeLog log = new PowerJobNativeLog();
        log.setAvailable(true);
        log.setContent(content);
        return log;
    }

    public static PowerJobNativeLog unavailable(String errorSummary) {
        PowerJobNativeLog log = new PowerJobNativeLog();
        log.setAvailable(false);
        log.setErrorSummary(errorSummary);
        return log;
    }
}
