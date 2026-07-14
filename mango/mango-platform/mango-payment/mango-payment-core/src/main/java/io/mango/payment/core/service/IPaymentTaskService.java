package io.mango.payment.core.service;

public interface IPaymentTaskService {
    io.mango.payment.api.vo.PaymentTaskDispatchResultVO expireOpenPaymentOrders(long limit);
    io.mango.payment.api.vo.PaymentTaskDispatchResultVO queryProcessingPaymentOrders(long limit);
}
