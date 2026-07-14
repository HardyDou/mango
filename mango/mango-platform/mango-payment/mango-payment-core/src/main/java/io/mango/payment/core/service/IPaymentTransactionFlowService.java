package io.mango.payment.core.service;

public interface IPaymentTransactionFlowService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentTransactionFlowVO> pageTransactionFlows(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentTransactionFlowVO detailTransactionFlow(Long id);
}
