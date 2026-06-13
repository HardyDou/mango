package io.mango.payment.starter.workflow;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.core.service.PaymentRefundApprovalWorkflowDefinitionService;
import io.mango.payment.starter.PaymentProperties;
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
@ConditionalOnProperty(prefix = "mango.payment.workflow.refund-approval.initializer", name = "enabled",
        havingValue = "true")
public class PaymentRefundApprovalWorkflowDefinitionInitializer implements ApplicationRunner {

    private final PaymentRefundApprovalWorkflowDefinitionService workflowDefinitionService;
    private final PaymentProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        PaymentProperties.InitializerProperties initializer = properties.getWorkflow().getRefundApproval().getInitializer();
        requireInitializerProperties(initializer);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withSecurity(
                    initializer.getSystemUserId(),
                    String.valueOf(initializer.getSystemTenantId()),
                    initializer.getPrincipalName(),
                    initializer.getRealm(),
                    initializer.getActorType(),
                    initializer.getPartyType(),
                    initializer.getPartyId(),
                    initializer.getAppCode()));
            ensureRefundApprovalDefinition();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private void requireInitializerProperties(PaymentProperties.InitializerProperties initializer) {
        Require.notNull(initializer.getSystemTenantId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少系统租户");
        Require.notNull(initializer.getSystemUserId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少系统用户");
        Require.notBlank(initializer.getPrincipalName(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少系统主体名称");
        Require.notBlank(initializer.getRealm(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少安全域");
        Require.notBlank(initializer.getActorType(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少参与者类型");
        Require.notBlank(initializer.getPartyType(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少主体类型");
        Require.notNull(initializer.getPartyId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少主体ID");
        Require.notBlank(initializer.getAppCode(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "支付退款审批流程初始化缺少应用编码");
    }

    private void ensureRefundApprovalDefinition() {
        R<WorkflowDeployVO> deployResult = workflowDefinitionService.ensureRefundApprovalDefinition();
        if (!deployResult.isSuccess()) {
            log.warn("支付内置退款审批流程发布失败，definitionKey={}, msg={}",
                    PaymentRefundApprovalWorkflowDefinitionService.REFUND_APPROVAL_DEFINITION_KEY, deployResult.getMsg());
        }
    }
}
