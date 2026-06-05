package io.mango.job.core.service.engine;

import lombok.Getter;
import lombok.Setter;

/**
 * Mango Job 引擎同步结果。
 */
@Getter
@Setter
public class MangoJobEngineResult {

    private boolean success;

    private String engineAppId;

    private String engineJobId;

    private String engineInstanceId;

    private String errorSummary;

    public static MangoJobEngineResult success(String engineAppId, String engineJobId) {
        MangoJobEngineResult result = new MangoJobEngineResult();
        result.setSuccess(true);
        result.setEngineAppId(engineAppId);
        result.setEngineJobId(engineJobId);
        return result;
    }

    public static MangoJobEngineResult triggerSuccess(String engineInstanceId) {
        MangoJobEngineResult result = new MangoJobEngineResult();
        result.setSuccess(true);
        result.setEngineInstanceId(engineInstanceId);
        return result;
    }

    public static MangoJobEngineResult success() {
        MangoJobEngineResult result = new MangoJobEngineResult();
        result.setSuccess(true);
        return result;
    }

    public static MangoJobEngineResult failed(String errorSummary) {
        MangoJobEngineResult result = new MangoJobEngineResult();
        result.setSuccess(false);
        result.setErrorSummary(errorSummary);
        return result;
    }
}
