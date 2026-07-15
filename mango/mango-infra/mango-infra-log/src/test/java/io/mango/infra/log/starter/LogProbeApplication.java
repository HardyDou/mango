package io.mango.infra.log.starter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import io.mango.infra.log.Loggers;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 独立子 JVM 日志探针，避免测试进程自己的 Logback 配置污染生产配置验收。
 */
@SpringBootApplication
public class LogProbeApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LogProbeApplication.class, args);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger mango = loggerContext.getLogger("io.mango");
        System.out.println("PROBE_ROOT_LEVEL=" + root.getLevel());
        System.out.println("PROBE_MANGO_LEVEL=" + mango.getLevel());
        printMaxHistory("PROBE_FILE_MAX_HISTORY", root.getAppender("FILE_JSON"));
        Logger operation = loggerContext.getLogger(Loggers.OPERATION);
        printMaxHistory("PROBE_OPERATION_MAX_HISTORY", operation.getAppender("OPERATION_LOG"));

        MDC.put("requestId", "request-flow");
        MDC.put("traceId", "trace-flow");
        MDC.put("clientIp", "127.0.0.1");
        LoggerFactory.getLogger("probe.regular").error("regular-flow");
        LoggerFactory.getLogger(Loggers.OPERATION).info("operation-flow");
        MDC.clear();

        context.close();
        loggerContext.stop();
    }

    private static void printMaxHistory(String name, Appender<?> appender) {
        if (appender instanceof RollingFileAppender<?> rollingFile
                && rollingFile.getRollingPolicy() instanceof TimeBasedRollingPolicy<?> policy) {
            System.out.println(name + "=" + policy.getMaxHistory());
        }
    }
}
