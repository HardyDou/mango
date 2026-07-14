package io.mango.payment.core.service;

public interface IPaymentOperationAuditService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentOperationAuditVO> pageOperationAudits(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
}
