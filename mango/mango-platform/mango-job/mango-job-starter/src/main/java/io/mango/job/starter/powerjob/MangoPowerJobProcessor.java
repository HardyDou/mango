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

    private final boolean captureConsoleOutput;

    public MangoPowerJobProcessor(IMangoJobHandlerRegistry handlerRegistry) {
        this(handlerRegistry, false);
    }

    public MangoPowerJobProcessor(IMangoJobHandlerRegistry handlerRegistry, boolean captureConsoleOutput) {
        this.handlerRegistry = handlerRegistry;
        this.captureConsoleOutput = captureConsoleOutput;
    }

    @Override
    public ProcessResult process(TaskContext taskContext) {
        try {
            if (captureConsoleOutput) {
                return MangoPowerJobExecutionLogBridge.capture(taskContext.getOmsLogger(), () -> doProcess(taskContext));
            }
            return doProcess(taskContext);
        } catch (RuntimeException ex) {
            writeFailureLog(taskContext, ex);
            return new ProcessResult(false, ex.getMessage());
        } catch (Exception ex) {
            writeFailureLog(taskContext, ex);
            return new ProcessResult(false, ex.getMessage());
        }
    }

    private ProcessResult doProcess(TaskContext taskContext) {
        try {
            PowerJobMangoPayload.JobParams jobParams =
                    PowerJobMangoPayload.readJobParams(taskContext.getJobParams());
            PowerJobMangoPayload.InstanceParams instanceParams =
                    PowerJobMangoPayload.readInstanceParams(taskContext.getInstanceParams());
            MangoJobHandler handler = handlerRegistry.findHandler(jobParams.getAppCode(), jobParams.getHandlerName())
                    .orElseThrow(() -> new IllegalStateException("Mango Job 处理器未注册：" + jobParams.getHandlerName()));
            MangoJobHandleResult result = handler.handle(toMangoContext(taskContext, jobParams, instanceParams));
            String message = resultMessage(result);
            if (result == null || result.getStatus() == JobHandleStatus.SUCCESS) {
                writeOutputLog(taskContext, result);
                return new ProcessResult(true, message);
            }
            writeOutputLog(taskContext, result);
            return new ProcessResult(false, message);
        } catch (RuntimeException ex) {
            writeFailureLog(taskContext, ex);
            return new ProcessResult(false, ex.getMessage());
        }
    }

    private void writeFailureLog(TaskContext taskContext, Exception ex) {
        if (taskContext.getOmsLogger() != null) {
            taskContext.getOmsLogger().error("Mango Job processor failed, jobId={}, instanceId={}",
                    taskContext.getJobId(), taskContext.getInstanceId(), ex);
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
        context.setTriggerType(resolveTriggerType(instanceParams));
        context.setTriggerBatchNo(instanceParams.getTriggerBatchNo());
        context.setParameter(resolveParameter(instanceParams, jobParams));
        context.setTraceId(String.valueOf(taskContext.getInstanceId()));
        return context;
    }

    private JobTriggerType resolveTriggerType(PowerJobMangoPayload.InstanceParams instanceParams) {
        if (instanceParams.getMangoInstanceId() == null) {
            return JobTriggerType.SCHEDULED;
        }
        return JobTriggerType.MANUAL;
    }

    private String resolveParameter(PowerJobMangoPayload.InstanceParams instanceParams,
                                    PowerJobMangoPayload.JobParams jobParams) {
        if (StringUtils.hasText(instanceParams.getParameter())) {
            return instanceParams.getParameter();
        }
        return jobParams.getParameter();
    }

    private String resultMessage(MangoJobHandleResult result) {
        if (result == null) {
            return "SUCCESS";
        }
        String message = StringUtils.hasText(result.getMessage()) ? result.getMessage() : result.getStatus().name();
        if (!StringUtils.hasText(result.getResult())) {
            return message;
        }
        return message + System.lineSeparator() + result.getResult();
    }

    private void writeOutputLog(TaskContext taskContext, MangoJobHandleResult result) {
        if (result == null || taskContext.getOmsLogger() == null) {
            return;
        }
        if (StringUtils.hasText(result.getMessage())) {
            taskContext.getOmsLogger().info("Mango Job handler message: {}", result.getMessage());
        }
        if (StringUtils.hasText(result.getResult())) {
            taskContext.getOmsLogger().info("Mango Job handler output: {}", result.getResult());
        }
    }
}
