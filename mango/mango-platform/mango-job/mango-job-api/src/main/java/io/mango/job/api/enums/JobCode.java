package io.mango.job.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Job 模块业务码。
 */
@Getter
@AllArgsConstructor
public enum JobCode implements BizCode {

    /** Job 请求参数、状态或上下文不正确。 */
    JOB_INVALID(400, "Job 请求参数不正确"),

    /** Job 资源不存在或不属于当前租户。 */
    JOB_NOT_FOUND(404, "Job 资源不存在"),

    /** Job 内部执行失败。 */
    JOB_INTERNAL_ERROR(500, "Job 内部执行失败");

    private final int code;
    private final String message;
}
