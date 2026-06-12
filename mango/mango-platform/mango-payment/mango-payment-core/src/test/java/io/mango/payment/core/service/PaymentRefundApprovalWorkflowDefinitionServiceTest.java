package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRefundApprovalWorkflowDefinitionServiceTest {

    private final WorkflowDefinitionApi workflowDefinitionApi = mock(WorkflowDefinitionApi.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentRefundApprovalWorkflowDefinitionService service =
            new PaymentRefundApprovalWorkflowDefinitionService(workflowDefinitionApi, objectMapper);

    @Test
    void ensureRefundApprovalDefinition_shouldSubmitPaymentOwnedWorkflowDefinitionContract() throws Exception {
        WorkflowDeployVO deployVO = new WorkflowDeployVO();
        deployVO.setDeploymentId("deploy-payment-refund");
        when(workflowDefinitionApi.ensurePublished(any(EnsureWorkflowDefinitionCommand.class)))
                .thenReturn(R.ok(deployVO));

        R<WorkflowDeployVO> result = service.ensureRefundApprovalDefinition();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getDeploymentId()).isEqualTo("deploy-payment-refund");
        ArgumentCaptor<EnsureWorkflowDefinitionCommand> captor =
                ArgumentCaptor.forClass(EnsureWorkflowDefinitionCommand.class);
        verify(workflowDefinitionApi).ensurePublished(captor.capture());
        EnsureWorkflowDefinitionCommand command = captor.getValue();
        assertThat(command.getDomainCode()).isEqualTo("PAYMENT");
        assertThat(command.getCategoryCode()).isEqualTo("PAYMENT_BUILTIN");
        assertThat(command.getCategoryName()).isEqualTo("支付内置流程");
        assertThat(command.getDefinitionKey()).isEqualTo("PAYMENT_REFUND_APPROVAL");
        assertThat(command.getDefinitionName()).isEqualTo("退款审批");
        assertThat(command.getFormCode()).isEqualTo("payment_refund_approval");
        JsonNode formJson = objectMapper.readTree(command.getFormJson());
        assertThat(formJson.path("mode").asText()).isEqualTo("CUSTOM");
        assertThat(formJson.path("customConfig").path("viewPath").asText()).isEqualTo("/payment/refund-approvals");
        assertThat(formJson.path("customConfig").path("applyPageKey").asText())
                .isEqualTo("payment.refundApproval.apply");
        assertThat(formJson.path("customConfig").path("approvePageKey").asText())
                .isEqualTo("payment.refundApproval.approve");
        JsonNode designerJson = objectMapper.readTree(command.getDesignerJson());
        assertThat(designerJson.path("childNode").path("nodeName").asText()).isEqualTo("默认自动通过");
        assertThat(designerJson.path("childNode").path("nodeType").asText()).isEqualTo("SERVICE");
    }
}
