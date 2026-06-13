package io.mango.payment.core.service;

import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentOrderStatusFlowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
class PaymentOrderViewSupport {

    private final PaymentOrderStatusFlowService statusFlowService;

    List<PaymentOrderStatusFlowVO> listBusinessStatusFlows(Long orderId) {
        return listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS, orderId, PaymentBusinessOrderStatusEnum::labelOf);
    }

    List<PaymentOrderStatusFlowVO> listPaymentStatusFlows(Long orderId) {
        return listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT, orderId, PaymentOrderStatusEnum::labelOf);
    }

    List<PaymentOrderStatusFlowVO> listRefundStatusFlows(Long orderId) {
        return listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_REFUND, orderId, this::refundStatusName);
    }

    boolean isExpiredOpenBusinessOrder(String status, LocalDateTime expireTime) {
        return expireTime != null
                && !expireTime.isAfter(LocalDateTime.now())
                && (PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(status)
                || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(status));
    }

    boolean isExpiredOpenPaymentOrder(String status, LocalDateTime expireTime) {
        return expireTime != null
                && !expireTime.isAfter(LocalDateTime.now())
                && (PaymentOrderStatusEnum.CREATED.getCode().equals(status)
                || PaymentOrderStatusEnum.PAYING.getCode().equals(status));
    }

    String refundStatusName(String status) {
        return PaymentRefundOrderStatusEnum.labelOf(normalizeRefundStatus(status));
    }

    String normalizeRefundStatus(String status) {
        return "PROCESSING".equals(status) ? PaymentRefundOrderStatusEnum.REFUNDING.getCode() : status;
    }

    String transactionFlowTypeName(String flowType) {
        if ("PAY_SUCCESS".equals(flowType) || "PAYMENT".equals(flowType)) {
            return "支付成功收入";
        }
        if ("REFUND_SUCCESS".equals(flowType) || "REFUND".equals(flowType)) {
            return "退款成功支出";
        }
        if ("PAYMENT_PENDING".equals(flowType)) {
            return "支付待确认备注";
        }
        if ("REFUND_PENDING".equals(flowType)) {
            return "退款待确认备注";
        }
        if ("CHANNEL_FEE".equals(flowType)) {
            return "通道手续费";
        }
        if ("ADJUST_NOTE".equals(flowType)) {
            return "差异处理备注";
        }
        return flowType;
    }

    private List<PaymentOrderStatusFlowVO> listStatusFlows(
            String orderType,
            Long orderId,
            Function<String, String> labelResolver) {
        List<PaymentOrderStatusFlowVO> flows = statusFlowService.list(PaymentContextSupport.currentTenantId(), orderType, orderId);
        flows.forEach(flow -> {
            String statusCode = StringUtils.hasText(flow.getToStatus()) ? flow.getToStatus() : flow.getStatusCode();
            flow.setStatusCode(statusCode);
            flow.setStatusName(labelResolver.apply(statusCode));
            flow.setSource(statusFlowSourceName(flow.getTriggerSource()));
        });
        return flows;
    }

    private String statusFlowSourceName(String triggerSource) {
        if (PaymentOrderStatusFlowService.SOURCE_OPENAPI_CREATE.equals(triggerSource)) {
            return "开放接口创建";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY.equals(triggerSource)) {
            return "收银台支付";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_QUERY.equals(triggerSource)) {
            return "主动查单";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK.equals(triggerSource)) {
            return "通道回调";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_CLOSE.equals(triggerSource)) {
            return "受控关单";
        }
        if (PaymentOrderStatusFlowService.SOURCE_OPENAPI_REFUND.equals(triggerSource)) {
            return "开放接口退款";
        }
        if (PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL.equals(triggerSource)) {
            return "后台退款审批";
        }
        if (PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY.equals(triggerSource)) {
            return "主动查退款";
        }
        if (PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE.equals(triggerSource)) {
            return "对账补偿";
        }
        if ("HISTORY_BACKFILL".equals(triggerSource)) {
            return "历史数据初始化";
        }
        return triggerSource;
    }
}
