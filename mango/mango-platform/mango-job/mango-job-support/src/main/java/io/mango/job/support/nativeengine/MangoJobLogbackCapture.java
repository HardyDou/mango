package io.mango.job.support.nativeengine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Captures Logback events emitted by the current job thread.
 */
final class MangoJobLogbackCapture implements AutoCloseable {

    private final Logger rootLogger;

    private final JobAppender appender;

    private MangoJobLogbackCapture(Logger rootLogger, JobAppender appender) {
        this.rootLogger = rootLogger;
        this.appender = appender;
    }

    static MangoJobLogbackCapture start() {
        org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (!(logger instanceof Logger logbackLogger)) {
            return new MangoJobLogbackCapture(null, null);
        }
        JobAppender jobAppender = new JobAppender(Thread.currentThread().getName());
        jobAppender.setName("mango-job-" + UUID.randomUUID());
        jobAppender.setContext(logbackLogger.getLoggerContext());
        jobAppender.start();
        logbackLogger.addAppender(jobAppender);
        return new MangoJobLogbackCapture(logbackLogger, jobAppender);
    }

    List<CapturedEvent> events() {
        if (appender == null) {
            return List.of();
        }
        return appender.events();
    }

    @Override
    public void close() {
        if (rootLogger == null || appender == null) {
            return;
        }
        rootLogger.detachAppender(appender);
        appender.stop();
    }

    record CapturedEvent(String level, String loggerName, String threadName, String message) {
    }

    private static final class JobAppender extends AppenderBase<ILoggingEvent> {

        private final String threadName;

        private final List<CapturedEvent> events = new ArrayList<>();

        private JobAppender(String threadName) {
            this.threadName = threadName;
        }

        @Override
        protected synchronized void append(ILoggingEvent eventObject) {
            if (eventObject == null || !threadName.equals(eventObject.getThreadName())) {
                return;
            }
            Level level = eventObject.getLevel();
            events.add(new CapturedEvent(level == null ? "INFO" : level.toString(),
                    eventObject.getLoggerName(), eventObject.getThreadName(), eventObject.getFormattedMessage()));
        }

        private synchronized List<CapturedEvent> events() {
            return List.copyOf(events);
        }
    }
}
