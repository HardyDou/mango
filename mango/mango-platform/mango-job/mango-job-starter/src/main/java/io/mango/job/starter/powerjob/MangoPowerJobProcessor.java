package io.mango.job.starter.powerjob;

import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import org.springframework.util.StringUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * PowerJob 内置处理器到 Mango Job 处理器的桥接器。
 */
public class MangoPowerJobProcessor implements BasicProcessor {

    public static final String PROCESSOR_NAME = "mangoPowerJobProcessor";

    private final IMangoJobHandlerRegistry handlerRegistry;

    public MangoPowerJobProcessor(IMangoJobHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public ProcessResult process(TaskContext taskContext) {
        try {
            PowerJobMangoPayload.JobParams jobParams =
                    PowerJobMangoPayload.readJobParams(taskContext.getJobParams());
            PowerJobMangoPayload.InstanceParams instanceParams =
                    PowerJobMangoPayload.readInstanceParams(taskContext.getInstanceParams());
            MangoJobHandler handler = handlerRegistry.findHandler(jobParams.getHandlerName())
                    .orElseThrow(() -> new IllegalStateException("Mango Job 处理器未注册：" + jobParams.getHandlerName()));
            MangoJobHandleResult result = handler.handle(toMangoContext(taskContext, jobParams, instanceParams));
            if (result == null || result.getStatus() == JobHandleStatus.SUCCESS) {
                return new ProcessResult(true, result == null ? "SUCCESS" : result.getMessage());
            }
            return new ProcessResult(false, result.getMessage());
        } catch (RuntimeException ex) {
            if (taskContext.getOmsLogger() != null) {
                taskContext.getOmsLogger().error("Mango Job processor failed, jobId={}, instanceId={}",
                        taskContext.getJobId(), taskContext.getInstanceId(), ex);
            }
            return new ProcessResult(false, ex.getMessage());
        }
    }

    private MangoJobHandleContext toMangoContext(TaskContext taskContext,
                                                PowerJobMangoPayload.JobParams jobParams,
                                                PowerJobMangoPayload.InstanceParams instanceParams) {
        MangoJobHandleContext context = new MangoJobHandleContext();
        context.setTenantId(jobParams.getTenantId());
        context.setAppCode(jobParams.getAppCode());
        context.setJobCode(jobParams.getJobCode());
        context.setInstanceId(instanceParams.getMangoInstanceId());
        context.setTriggerType(JobTriggerType.MANUAL);
        context.setTriggerBatchNo(instanceParams.getTriggerBatchNo());
        context.setParameter(resolveParameter(instanceParams, jobParams));
        context.setTraceId(String.valueOf(taskContext.getInstanceId()));
        return context;
    }

    private String resolveParameter(PowerJobMangoPayload.InstanceParams instanceParams,
                                    PowerJobMangoPayload.JobParams jobParams) {
        if (StringUtils.hasText(instanceParams.getParameter())) {
            return instanceParams.getParameter();
        }
        return jobParams.getParameter();
    }
}
