package io.mango.payment.starter.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.core.integration.PaymentRemoteOutcome;
import io.mango.payment.core.service.PaymentRefundApprovalWorkflowPublisher;
import io.mango.payment.starter.PaymentProperties;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PaymentRefundApprovalWorkflowDefinitionInitializerTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("run should initialize refund approval workflow with configured system context")
    void run_initializesWithConfiguredSystemContext() {
        TestWorkflowDefinitionService service = new TestWorkflowDefinitionService();
        PaymentRefundApprovalWorkflowDefinitionInitializer initializer =
                new PaymentRefundApprovalWorkflowDefinitionInitializer(service, properties());

        initializer.run(null);

        assertThat(service.called).isTrue();
        assertThat(service.tenantId).isEqualTo("99");
        assertThat(service.userId).isEqualTo(8801L);
        assertThat(service.principalName).isEqualTo("payment-workflow");
        assertThat(service.appCode).isEqualTo("payment-starter");
        assertThat(MangoContextHolder.get().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("run should fail fast when enabled initializer misses system tenant")
    void run_missingSystemTenant_failsFast() {
        PaymentProperties properties = properties();
        properties.getWorkflow().getRefundApproval().getInitializer().setSystemTenantId(null);
        PaymentRefundApprovalWorkflowDefinitionInitializer initializer =
                new PaymentRefundApprovalWorkflowDefinitionInitializer(new TestWorkflowDefinitionService(), properties);

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode());
    }

    private PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        PaymentProperties.InitializerProperties initializer =
                properties.getWorkflow().getRefundApproval().getInitializer();
        initializer.setEnabled(true);
        initializer.setSystemTenantId(99L);
        initializer.setSystemUserId(8801L);
        initializer.setPrincipalName("payment-workflow");
        initializer.setRealm("INTERNAL");
        initializer.setActorType("INTERNAL_USER");
        initializer.setPartyType("INTERNAL_ORG");
        initializer.setPartyId(8801L);
        initializer.setAppCode("payment-starter");
        return properties;
    }

    private static class TestWorkflowDefinitionService extends PaymentRefundApprovalWorkflowPublisher {

        private boolean called;
        private String tenantId;
        private Long userId;
        private String principalName;
        private String appCode;

        TestWorkflowDefinitionService() {
            super(mock(WorkflowDefinitionApi.class), new ObjectMapper());
        }

        @Override
        public PaymentRemoteOutcome<WorkflowDeployVO> ensureRefundApprovalDefinition() {
            called = true;
            tenantId = MangoContextHolder.tenantId();
            userId = MangoContextHolder.userId();
            principalName = MangoContextHolder.principalName();
            appCode = MangoContextHolder.appCode();
            return new PaymentRemoteOutcome<>(true, null, null);
        }
    }

}
