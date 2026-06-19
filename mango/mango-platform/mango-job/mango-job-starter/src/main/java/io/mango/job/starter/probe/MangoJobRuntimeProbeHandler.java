package io.mango.job.starter.probe;

import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

/**
 * Mango Job 运行时探针处理器。
 */
public class MangoJobRuntimeProbeHandler implements MangoJobHandler {

    public static final String HANDLER_NAME = "mangoJobRuntimeProbeHandler";

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoJobRuntimeProbeHandler.class);

    @Override
    public String handlerName() {
        return HANDLER_NAME;
    }

    @Override
    public String appCode() {
        return "mango-job";
    }

    @Override
    public String serviceCode() {
        return "mango-job";
    }

    @Override
    public String workerGroup() {
        return "mango-job";
    }

    @Override
    public MangoJobHandleResult handle(MangoJobHandleContext context) {
        String result = resultJson(context);
        System.out.println("Mango Job runtime probe System.out: " + result);
        LOGGER.info("Mango Job runtime probe logger: {}", result);
        if (shouldFail(context.getParameter())) {
            MangoJobHandleResult failed = MangoJobHandleResult.failed("Mango Job runtime probe failed by parameter");
            failed.setResult(result);
            return failed;
        }
        MangoJobHandleResult success = MangoJobHandleResult.success("Mango Job runtime probe executed");
        success.setResult(result);
        return success;
    }

    private boolean shouldFail(String parameter) {
        String value = parameter == null ? "" : parameter.replace(" ", "").toLowerCase();
        return value.contains("\"fail\":true");
    }

    private String resultJson(MangoJobHandleContext context) {
        return "{"
                + "\"handlerName\":\"" + HANDLER_NAME + "\","
                + "\"tenantId\":\"" + escape(context.getTenantId()) + "\","
                + "\"appCode\":\"" + escape(context.getAppCode()) + "\","
                + "\"jobCode\":\"" + escape(context.getJobCode()) + "\","
                + "\"mangoInstanceId\":\"" + value(context.getInstanceId()) + "\","
                + "\"triggerBatchNo\":\"" + escape(context.getTriggerBatchNo()) + "\","
                + "\"traceId\":\"" + escape(context.getTraceId()) + "\","
                + "\"hostName\":\"" + escape(hostName()) + "\","
                + "\"threadName\":\"" + escape(Thread.currentThread().getName()) + "\","
                + "\"executedAt\":\"" + LocalDateTime.now() + "\","
                + "\"parameter\":\"" + escape(context.getParameter()) + "\""
                + "}";
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    private String value(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escape(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
