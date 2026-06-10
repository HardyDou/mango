package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.vo.PaymentOrderStatusFlowVO;
import io.mango.payment.core.entity.PaymentOrderStatusFlowEntity;
import io.mango.payment.core.mapper.PaymentOrderStatusFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOrderStatusFlowService {

    public static final String ORDER_TYPE_BUSINESS = "BUSINESS_ORDER";
    public static final String ORDER_TYPE_PAYMENT = "PAYMENT_ORDER";
    public static final String ORDER_TYPE_REFUND = "REFUND_ORDER";

    public static final String SOURCE_OPENAPI_CREATE = "OPENAPI_CREATE";
    public static final String SOURCE_CASHIER_PAY = "CASHIER_PAY";
    public static final String SOURCE_CHANNEL_QUERY = "CHANNEL_QUERY";
    public static final String SOURCE_CHANNEL_CALLBACK = "CHANNEL_CALLBACK";
    public static final String SOURCE_CHANNEL_CLOSE = "CHANNEL_CLOSE";
    public static final String SOURCE_OPENAPI_REFUND = "OPENAPI_REFUND";
    public static final String SOURCE_MANUAL_REFUND_APPROVAL = "MANUAL_REFUND_APPROVAL";
    public static final String SOURCE_REFUND_QUERY = "REFUND_QUERY";
    public static final String SOURCE_RECONCILIATION_COMPENSATE = "RECONCILIATION_COMPENSATE";

    private final PaymentOrderStatusFlowMapper statusFlowMapper;

    public void record(
            Long tenantId,
            String orderType,
            Long orderId,
            String orderNo,
            String fromStatus,
            String toStatus,
            String triggerSource,
            String triggerNo,
            LocalDateTime happenTime,
            String remark) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(orderType, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "订单类型不能为空");
        Require.notNull(orderId, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "订单 ID 不能为空");
        Require.notBlank(orderNo, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "订单号不能为空");
        Require.notBlank(toStatus, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "目标状态不能为空");
        Require.notBlank(triggerSource, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "触发来源不能为空");
        LocalDateTime eventTime = happenTime == null ? LocalDateTime.now() : happenTime;

        PaymentOrderStatusFlowEntity entity = new PaymentOrderStatusFlowEntity();
        entity.setOrderType(orderType);
        entity.setOrderId(orderId);
        entity.setOrderNo(orderNo);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setTriggerSource(triggerSource);
        entity.setTriggerNo(PaymentContextSupport.trimToNull(triggerNo));
        entity.setOperatorId(PaymentContextSupport.currentUserId());
        entity.setOperatorName(PaymentContextSupport.currentPrincipalName());
        entity.setHappenTime(eventTime);
        entity.setRemark(PaymentContextSupport.trimToNull(remark));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(eventTime);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(eventTime);
        entity.setDelFlag(0);
        statusFlowMapper.insert(entity);
    }

    public List<PaymentOrderStatusFlowVO> list(Long tenantId, String orderType, Long orderId) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(orderType, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "订单类型不能为空");
        Require.notNull(orderId, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "订单 ID 不能为空");
        return statusFlowMapper.selectStatusFlows(tenantId, orderType, orderId);
    }
}
