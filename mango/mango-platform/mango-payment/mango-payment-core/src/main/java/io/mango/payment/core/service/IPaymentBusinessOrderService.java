package io.mango.payment.core.service;

public interface IPaymentBusinessOrderService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentBusinessOrderVO> pageBusinessOrders(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentBusinessOrderVO detailBusinessOrder(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentBusinessOrderStatusVO> listBusinessOrderStatuses();
    io.mango.payment.api.vo.PaymentBusinessOrderVO createBusinessOrder(
            io.mango.payment.api.command.CreatePaymentBusinessOrderCommand command);
}
