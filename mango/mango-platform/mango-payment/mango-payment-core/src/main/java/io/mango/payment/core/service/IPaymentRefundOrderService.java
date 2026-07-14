package io.mango.payment.core.service;

public interface IPaymentRefundOrderService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentRefundOrderVO> pageRefundOrders(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentRefundOrderVO detailRefundOrder(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentRefundOrderStatusVO> listRefundOrderStatuses();
    io.mango.payment.api.vo.PaymentRefundOrderVO queryRefundOrder(
            io.mango.payment.api.command.QueryPaymentRefundOrderCommand command);
}
