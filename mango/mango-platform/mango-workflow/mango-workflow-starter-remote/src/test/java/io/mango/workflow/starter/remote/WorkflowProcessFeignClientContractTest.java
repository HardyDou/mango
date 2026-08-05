package io.mango.workflow.starter.remote;

import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.command.WithdrawWorkflowProcessCommand;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowProcessFeignClientContractTest {

    @Test
    void withdraw_matchesPublicApiAndHttpBinding() throws NoSuchMethodException {
        Method apiMethod = WorkflowProcessApi.class
                .getDeclaredMethod("withdraw", WithdrawWorkflowProcessCommand.class);
        Method feignMethod = WorkflowProcessFeignClient.class
                .getDeclaredMethod("withdraw", WithdrawWorkflowProcessCommand.class);

        assertThat(WorkflowProcessApi.class.isAssignableFrom(WorkflowProcessFeignClient.class)).isTrue();
        assertThat(feignMethod.getGenericReturnType()).isEqualTo(apiMethod.getGenericReturnType());
        assertThat(feignMethod.getAnnotation(PostMapping.class).value()).containsExactly("/withdraw");
        assertThat(feignMethod.getParameters()[0].getAnnotation(RequestBody.class)).isNotNull();
    }

    @Test
    void feignClient_usesWorkflowProcessRootPath() {
        FeignClient feignClient = WorkflowProcessFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("mango-workflow");
        assertThat(feignClient.path()).isEqualTo("/workflow/processes");
    }
}
