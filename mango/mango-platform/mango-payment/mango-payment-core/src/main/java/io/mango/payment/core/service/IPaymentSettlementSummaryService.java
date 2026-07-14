package io.mango.payment.core.service;

public interface IPaymentSettlementSummaryService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentSettlementSummaryVO> pageSettlementSummaries(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentSettlementSummaryVO detailSettlementSummary(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO> listSettlementSummaryStatuses();
    io.mango.payment.api.vo.PaymentSettlementSummaryVO generateSettlementSummary(
            io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand command);
    io.mango.payment.api.vo.PaymentSettlementSummaryVO confirmSettlementSummary(
            io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand command);
    io.mango.payment.api.vo.PaymentSettlementSummaryVO voidSettlementSummary(
            io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand command);
}
