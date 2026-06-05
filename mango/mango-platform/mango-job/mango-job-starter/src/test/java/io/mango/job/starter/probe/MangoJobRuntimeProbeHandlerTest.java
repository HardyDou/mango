package io.mango.job.starter.probe;

import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MangoJobRuntimeProbeHandlerTest {

    private final MangoJobRuntimeProbeHandler handler = new MangoJobRuntimeProbeHandler();

    @Test
    void shouldReturnProbeHandlerName() {
        assertThat(handler.handlerName()).isEqualTo(MangoJobRuntimeProbeHandler.HANDLER_NAME);
    }

    @Test
    void shouldReturnRuntimeResultWhenProbeSucceeds() {
        MangoJobHandleContext context = context("{\"scene\":\"manual\"}");

        MangoJobHandleResult result = handler.handle(context);

        assertThat(result.getStatus()).isEqualTo(JobHandleStatus.SUCCESS);
        assertThat(result.getMessage()).isEqualTo("Mango Job runtime probe executed");
        assertThat(result.getResult())
                .contains("\"handlerName\":\"mangoJobRuntimeProbeHandler\"")
                .contains("\"jobCode\":\"acceptance-manual\"")
                .contains("\"parameter\":\"{\\\"scene\\\":\\\"manual\\\"}\"");
    }

    @Test
    void shouldReturnFailedWhenProbeParameterRequestsFailure() {
        MangoJobHandleContext context = context("{\"fail\":true}");

        MangoJobHandleResult result = handler.handle(context);

        assertThat(result.getStatus()).isEqualTo(JobHandleStatus.FAILED);
        assertThat(result.getMessage()).isEqualTo("Mango Job runtime probe failed by parameter");
        assertThat(result.getResult()).contains("\"parameter\":\"{\\\"fail\\\":true}\"");
    }

    private MangoJobHandleContext context(String parameter) {
        MangoJobHandleContext context = new MangoJobHandleContext();
        context.setTenantId("1");
        context.setAppCode("mango-job");
        context.setJobCode("acceptance-manual");
        context.setInstanceId(1001L);
        context.setTriggerBatchNo("batch-1001");
        context.setTraceId("trace-1001");
        context.setParameter(parameter);
        return context;
    }
}
