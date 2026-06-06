package io.mango.job.starter.powerjob;

import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.service.impl.MangoJobHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MangoPowerJobProcessorTest {

    @Test
    void shouldDispatchPowerJobTaskToMangoHandler() {
        RecordingHandler handler = new RecordingHandler();
        MangoPowerJobProcessor processor = new MangoPowerJobProcessor(
                new MangoJobHandlerRegistry(new SingleObjectProvider<>(handler)));
        TaskContext taskContext = new TaskContext();
        taskContext.setJobId(90001L);
        taskContext.setInstanceId(80001L);
        taskContext.setJobParams(PowerJobMangoPayload.jobParams(definition()));
        taskContext.setInstanceParams(PowerJobMangoPayload.instanceParams(definition(), instance(), "batch-1", null));
        RecordingOmsLogger omsLogger = new RecordingOmsLogger();
        taskContext.setOmsLogger(omsLogger);

        ProcessResult result = processor.process(taskContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMsg()).contains("done").contains("\"rows\":3");
        assertThat(omsLogger.infoMessages).anySatisfy(message ->
                assertThat(message).contains("Mango Job handler message").contains("done"));
        assertThat(omsLogger.infoMessages).anySatisfy(message ->
                assertThat(message).contains("Mango Job handler output").contains("\"rows\":3"));
        assertThat(handler.context.getTenantId()).isEqualTo("tenant-a");
        assertThat(handler.context.getAppCode()).isEqualTo("internal-admin");
        assertThat(handler.context.getJobCode()).isEqualTo("sync-user-status");
        assertThat(handler.context.getInstanceId()).isEqualTo(70001L);
        assertThat(handler.context.getTriggerBatchNo()).isEqualTo("batch-1");
        assertThat(handler.context.getParameter()).isEqualTo("{\"dryRun\":false}");
    }

    @Test
    void shouldBridgeSystemOutAndLoggerToOmsLogger() {
        LoggingHandler handler = new LoggingHandler();
        MangoPowerJobProcessor processor = new MangoPowerJobProcessor(
                new MangoJobHandlerRegistry(new SingleObjectProvider<>(handler)), true);
        TaskContext taskContext = new TaskContext();
        taskContext.setJobParams(PowerJobMangoPayload.jobParams(definition()));
        taskContext.setInstanceParams(PowerJobMangoPayload.instanceParams(definition(), instance(), "batch-1", null));
        RecordingOmsLogger omsLogger = new RecordingOmsLogger();
        taskContext.setOmsLogger(omsLogger);

        ProcessResult result = processor.process(taskContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(omsLogger.infoMessages).anySatisfy(message ->
                assertThat(message).contains("[System.out]").contains("stdout from job handler"));
        assertThat(omsLogger.infoMessages).anySatisfy(message ->
                assertThat(message).contains("[System.out]").contains("logger from job handler"));
        assertThat(omsLogger.infoMessages).noneMatch(message -> message.contains("[logger]"));
    }

    @Test
    void shouldReturnFailedWhenHandlerMissing() {
        MangoPowerJobProcessor processor = new MangoPowerJobProcessor(
                new MangoJobHandlerRegistry(new SingleObjectProvider<>()));
        TaskContext taskContext = new TaskContext();
        taskContext.setJobParams(PowerJobMangoPayload.jobParams(definition()));

        ProcessResult result = processor.process(taskContext);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMsg()).contains("Mango Job 处理器未注册");
    }

    private MangoJobDefinitionEntity definition() {
        MangoJobDefinitionEntity definition = new MangoJobDefinitionEntity();
        definition.setTenantId("tenant-a");
        definition.setAppCode("internal-admin");
        definition.setJobCode("sync-user-status");
        definition.setHandlerName("syncUserStatusJobHandler");
        definition.setParamValue("{\"dryRun\":false}");
        return definition;
    }

    private MangoJobInstanceEntity instance() {
        MangoJobInstanceEntity instance = new MangoJobInstanceEntity();
        instance.setId(70001L);
        return instance;
    }

    private static class RecordingHandler implements MangoJobHandler {

        private MangoJobHandleContext context;

        @Override
        public String handlerName() {
            return "syncUserStatusJobHandler";
        }

        @Override
        public MangoJobHandleResult handle(MangoJobHandleContext context) {
            this.context = context;
            MangoJobHandleResult result = MangoJobHandleResult.success("done");
            result.setResult("{\"rows\":3}");
            return result;
        }
    }

    private static class LoggingHandler implements MangoJobHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHandler.class);

        @Override
        public String handlerName() {
            return "syncUserStatusJobHandler";
        }

        @Override
        public MangoJobHandleResult handle(MangoJobHandleContext context) {
            System.out.println("stdout from job handler");
            LOGGER.info("logger from job handler");
            return MangoJobHandleResult.success("done");
        }
    }

    private static class RecordingOmsLogger implements OmsLogger {

        private final List<String> infoMessages = new ArrayList<>();

        @Override
        public void debug(String messagePattern, Object... args) {
        }

        @Override
        public void info(String messagePattern, Object... args) {
            infoMessages.add(format(messagePattern, args));
        }

        @Override
        public void warn(String messagePattern, Object... args) {
        }

        @Override
        public void error(String messagePattern, Object... args) {
        }

        private String format(String messagePattern, Object... args) {
            String value = messagePattern;
            if (args == null) {
                return value;
            }
            for (Object arg : args) {
                int index = value.indexOf("{}");
                if (index < 0) {
                    break;
                }
                value = value.substring(0, index)
                        + (arg == null ? "null" : arg)
                        + value.substring(index + 2);
            }
            return value;
        }
    }

    private static class SingleObjectProvider<T> implements ObjectProvider<T> {

        private final List<T> values;

        @SafeVarargs
        SingleObjectProvider(T... values) {
            this.values = List.of(values);
        }

        @Override
        public T getObject(Object... args) {
            return values.getFirst();
        }

        @Override
        public T getIfAvailable() {
            return values.isEmpty() ? null : values.getFirst();
        }

        @Override
        public T getIfUnique() {
            return getIfAvailable();
        }

        @Override
        public T getObject() {
            return values.getFirst();
        }

        @Override
        public Iterator<T> iterator() {
            return values.iterator();
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            values.forEach(action);
        }

        @Override
        public Stream<T> stream() {
            return values.stream();
        }

        @Override
        public Stream<T> orderedStream() {
            return values.stream();
        }
    }
}
