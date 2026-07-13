package io.mango.payment.core.service;

public interface IPaymentRefundApprovalService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentRefundApprovalVO> pageRefundApprovals(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentRefundApprovalVO detailRefundApproval(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentRefundApprovalStatusVO> listRefundApprovalStatuses();
    io.mango.payment.api.vo.PaymentRefundApprovalVO createRefundApproval(
            io.mango.payment.api.command.CreatePaymentRefundApprovalCommand command);
}
