package io.mango.payment.core.service;

public interface IPaymentExceptionOrderService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentExceptionOrderVO> pageExceptionOrders(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentExceptionOrderVO detailExceptionOrder(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentExceptionOrderStatusVO> listExceptionOrderStatuses();
    java.util.List<io.mango.payment.api.vo.PaymentExceptionOrderActionVO> listExceptionOrderActions();
    io.mango.payment.api.vo.PaymentExceptionOrderVO handleExceptionOrder(
            io.mango.payment.api.command.HandlePaymentExceptionOrderCommand command);
}
