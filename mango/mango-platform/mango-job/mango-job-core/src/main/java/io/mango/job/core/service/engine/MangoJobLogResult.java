package io.mango.job.core.service.engine;

import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎日志查询结果。
 */
@Getter
@Setter
public class MangoJobLogResult {

    private boolean success;

    private String source;

    private boolean nativeLogAvailable;

    private String logFetchStatus;

    private String nativeLogContent;

    private String content;

    private String engineResult;

    private String errorSummary;

    public static MangoJobLogResult success(String source, String nativeLogContent, String engineResult) {
        MangoJobLogResult result = new MangoJobLogResult();
        result.setSuccess(true);
        result.setSource(source);
        result.setNativeLogAvailable(true);
        result.setLogFetchStatus("AVAILABLE");
        result.setNativeLogContent(nativeLogContent);
        result.setContent(nativeLogContent);
        result.setEngineResult(engineResult);
        return result;
    }

    public static MangoJobLogResult unavailable(String source, String errorSummary, String engineResult) {
        MangoJobLogResult result = new MangoJobLogResult();
        result.setSuccess(false);
        result.setSource(source);
        result.setNativeLogAvailable(false);
        result.setLogFetchStatus("UNAVAILABLE");
        result.setErrorSummary(errorSummary);
        result.setEngineResult(engineResult);
        return result;
    }

    public static MangoJobLogResult failed(String source, String errorSummary) {
        return unavailable(source, errorSummary, null);
    }
}
