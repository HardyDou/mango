package io.mango.payment.starter.workflow;

import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.core.service.PaymentRefundApprovalWorkflowDefinitionService;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initializes payment-owned workflow definitions.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@RequiredArgsConstructor
@ConditionalOnBean(PaymentRefundApprovalWorkflowDefinitionService.class)
@ConditionalOnProperty(prefix = "mango.payment.workflow", name = "refund-approval-initializer-enabled",
        havingValue = "true", matchIfMissing = true)
public class PaymentRefundApprovalWorkflowDefinitionInitializer implements ApplicationRunner {

    private static final Long SYSTEM_TENANT_ID = 1L;
    private static final Long SYSTEM_USER_ID = 1L;

    private final PaymentRefundApprovalWorkflowDefinitionService workflowDefinitionService;

    @Override
    public void run(ApplicationArguments args) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withSecurity(SYSTEM_USER_ID, String.valueOf(SYSTEM_TENANT_ID),
                    "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", SYSTEM_TENANT_ID, "internal-admin"));
            ensureRefundApprovalDefinition();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private void ensureRefundApprovalDefinition() {
        R<WorkflowDeployVO> deployResult = workflowDefinitionService.ensureRefundApprovalDefinition();
        if (!deployResult.isSuccess()) {
            log.warn("支付内置退款审批流程发布失败，definitionKey={}, msg={}",
                    PaymentRefundApprovalWorkflowDefinitionService.REFUND_APPROVAL_DEFINITION_KEY, deployResult.getMsg());
        }
    }
}
