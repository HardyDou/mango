package io.mango.job.api.handler;

import io.mango.job.api.enums.JobHandleStatus;
import lombok.Data;

/**
 * Mango Job 处理器执行结果。
 */
@Data
public class MangoJobHandleResult {

    /**
     * 执行状态。
     */
    private JobHandleStatus status;

    /**
     * 结果摘要。
     */
    private String message;

    /**
     * 结果数据 JSON。
     */
    private String result;

    /**
     * 成功结果。
     */
    public static MangoJobHandleResult success(String message) {
        MangoJobHandleResult result = new MangoJobHandleResult();
        result.setStatus(JobHandleStatus.SUCCESS);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败结果。
     */
    public static MangoJobHandleResult failed(String message) {
        MangoJobHandleResult result = new MangoJobHandleResult();
        result.setStatus(JobHandleStatus.FAILED);
        result.setMessage(message);
        return result;
    }
}
