package io.mango.workflow.starter.remote;

import io.mango.workflow.api.WorkflowBusinessApplyApi;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowBusinessApplyFeignClientContractTest {

    @Test
    void internalProcessLookupMatchesApiAndHttpBinding() throws NoSuchMethodException {
        Method apiMethod = WorkflowBusinessApplyApi.class
                .getDeclaredMethod("findByProcessInstance", String.class);
        Method feignMethod = WorkflowBusinessApplyFeignClient.class
                .getDeclaredMethod("findByProcessInstance", String.class);

        assertThat(WorkflowBusinessApplyApi.class
                .isAssignableFrom(WorkflowBusinessApplyFeignClient.class)).isTrue();
        assertThat(feignMethod.getGenericReturnType()).isEqualTo(apiMethod.getGenericReturnType());
        assertThat(feignMethod.getAnnotation(GetMapping.class).value())
                .containsExactly("/internal/by-process-instance");
        assertThat(feignMethod.getParameters()[0].getAnnotation(RequestParam.class).value())
                .isEqualTo("processInstanceId");
    }

    @Test
    void feignClientUsesWorkflowBusinessApplyRootPath() {
        FeignClient feignClient = WorkflowBusinessApplyFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("mango-workflow");
        assertThat(feignClient.path()).isEqualTo("/workflow/business-applies");
    }
}
