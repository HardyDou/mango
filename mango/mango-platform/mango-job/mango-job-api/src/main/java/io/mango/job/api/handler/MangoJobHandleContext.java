package io.mango.job.api.handler;

import io.mango.job.api.enums.JobTriggerType;
import lombok.Data;

/**
 * Mango Job 处理器执行上下文。
 */
@Data
public class MangoJobHandleContext {

    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 逻辑应用编码。
     */
    private String appCode;

    /**
     * 任务编码。
     */
    private String jobCode;

    /**
     * 任务实例 ID。
     */
    private Long instanceId;

    /**
     * 操作人 ID。
     */
    private Long operatorId;

    /**
     * 触发来源。
     */
    private JobTriggerType triggerType;

    /**
     * 触发批次号。
     */
    private String triggerBatchNo;

    /**
     * 链路追踪 ID。
     */
    private String traceId;

    /**
     * 任务参数 JSON。
     */
    private String parameter;
}
