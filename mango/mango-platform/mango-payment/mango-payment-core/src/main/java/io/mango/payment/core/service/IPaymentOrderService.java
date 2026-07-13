package io.mango.payment.core.service;

public interface IPaymentOrderService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentOrderVO> pagePaymentOrders(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentOrderVO detailPaymentOrder(Long id);
    io.mango.payment.api.vo.PaymentOrderSyncStatusVO syncPaymentOrderStatus(String payOrderNo);
    java.util.List<io.mango.payment.api.vo.PaymentOrderStatusVO> listPaymentOrderStatuses();
}
