package io.mango.job.support.nativeengine;

import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.api.vo.MangoJobWorkerExecutionLogVO;
import io.mango.job.support.service.IMangoJobHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.PrintStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mango Job Worker 本地处理器执行器。
 */
@Service
public class MangoJobWorkerExecutor {

    private static final String LINE_BREAK_REGEX = "\\R";

    private static final Pattern LOGBACK_CONSOLE_LINE = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[[^]]+] \\w+\\s+\\S+ - .*");

    private static final Pattern SPRING_BOOT_CONSOLE_LINE = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*\\s---\\s\\[[^]]+]\\s.*: .*");

    private final IMangoJobHandlerRegistry handlerRegistry;

    public MangoJobWorkerExecutor(IMangoJobHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerExecuteCommand command, String workerAddress) {
        Require.notNull(command, "Worker 执行命令不能为空");
        String ownerService = StringUtils.hasText(command.getOwnerService())
                ? command.getOwnerService().trim() : command.getAppCode();
        String workerGroup = StringUtils.hasText(command.getWorkerGroup())
                ? command.getWorkerGroup().trim() : ownerService;
        MangoJobHandler handler = handlerRegistry.findHandler(command.getAppCode(), ownerService,
                        workerGroup, command.getHandlerName(), command.getJobCode())
                .orElseGet(() -> Require.fail(404, "Job 处理器未注册或归属不匹配："
                        + ownerService + "/" + workerGroup + "/"
                        + command.getAppCode() + "/" + command.getHandlerName() + "/" + command.getJobCode()));
        MangoContextSnapshot previous = MangoContextHolder.get();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        MangoJobExecutionLogBuffer buffer = new MangoJobExecutionLogBuffer();
        MangoJobLogbackCapture logbackCapture = MangoJobLogbackCapture.start();
        try {
            MangoContextHolder.set(MangoContextSnapshot.empty()
                    .withRequest(command.getTraceId(), command.getTraceId(), command.getTenantId(),
                            command.getAppCode(), workerAddress)
                    .withSecurity(command.getOperatorId(), command.getTenantId(), "job-system",
                            "SYSTEM", "JOB", "SYSTEM", command.getOperatorId(), command.getAppCode()));
            System.setOut(new MangoJobTeePrintStream(originalOut, buffer.stdoutStream()));
            System.setErr(new MangoJobTeePrintStream(originalErr, buffer.stderrStream()));
            MangoJobHandleResult result = handler.handle(toHandleContext(command));
            System.out.flush();
            System.err.flush();
            return toResult(result, workerAddress, buffer, logbackCapture);
        } catch (RuntimeException ex) {
            System.out.flush();
            System.err.flush();
            MangoJobWorkerExecuteResultVO result = toResult(MangoJobHandleResult.failed(ex.getMessage()),
                    workerAddress, buffer, logbackCapture);
            result.setStatus(JobHandleStatus.FAILED);
            return result;
        } finally {
            logbackCapture.close();
            System.setOut(originalOut);
            System.setErr(originalErr);
            MangoContextHolder.set(previous);
        }
    }

    private MangoJobHandleContext toHandleContext(MangoJobWorkerExecuteCommand command) {
        MangoJobHandleContext context = new MangoJobHandleContext();
        context.setTenantId(command.getTenantId());
        context.setAppCode(command.getAppCode());
        context.setJobCode(command.getJobCode());
        context.setInstanceId(command.getInstanceId());
        context.setOperatorId(command.getOperatorId());
        context.setTriggerType(command.getTriggerType());
        context.setTriggerBatchNo(command.getTriggerBatchNo());
        context.setTraceId(command.getTraceId());
        context.setParameter(command.getParameter());
        return context;
    }

    private MangoJobWorkerExecuteResultVO toResult(MangoJobHandleResult handleResult,
                                                   String workerAddress,
                                                   MangoJobExecutionLogBuffer buffer,
                                                   MangoJobLogbackCapture logbackCapture) {
        MangoJobWorkerExecuteResultVO result = new MangoJobWorkerExecuteResultVO();
        result.setWorkerAddress(workerAddress);
        result.setStatus(handleResult == null || handleResult.getStatus() == null
                ? JobHandleStatus.SUCCESS : handleResult.getStatus());
        result.setMessage(handleResult == null ? null : handleResult.getMessage());
        result.setResult(handleResult == null ? null : handleResult.getResult());
        addMultilineLog(result, "INFO", "System.out", buffer.stdout(), logbackCapture.events());
        addMultilineLog(result, "ERROR", "System.err", buffer.stderr());
        logbackCapture.events().forEach(event -> addLog(result, event.level(), event.loggerName(), event.message()));
        return result;
    }

    private void addMultilineLog(MangoJobWorkerExecuteResultVO result,
                                 String level,
                                 String loggerName,
                                 String content) {
        addMultilineLog(result, level, loggerName, content, List.of());
    }

    private void addMultilineLog(MangoJobWorkerExecuteResultVO result,
                                 String level,
                                 String loggerName,
                                 String content,
                                 List<MangoJobLogbackCapture.CapturedEvent> capturedEvents) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        for (String line : content.split(LINE_BREAK_REGEX)) {
            if (StringUtils.hasText(line) && !isLogbackConsoleMirror(line, capturedEvents)) {
                addLog(result, level, loggerName, line);
            }
        }
    }

    private boolean isLogbackConsoleMirror(String line, List<MangoJobLogbackCapture.CapturedEvent> capturedEvents) {
        if (!LOGBACK_CONSOLE_LINE.matcher(line).matches()
                && !SPRING_BOOT_CONSOLE_LINE.matcher(line).matches()) {
            return false;
        }
        return capturedEvents.stream()
                .anyMatch(event -> StringUtils.hasText(event.message()) && line.contains(event.message()));
    }

    private void addLog(MangoJobWorkerExecuteResultVO result, String level, String loggerName, String content) {
        MangoJobWorkerExecutionLogVO log = new MangoJobWorkerExecutionLogVO();
        log.setLevel(level);
        log.setLoggerName(loggerName);
        log.setContent(content);
        result.getLogs().add(log);
    }
}
