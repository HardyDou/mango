package io.mango.payment.core.service;

public interface IPaymentDifferenceService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentDifferenceVO> pageDifferences(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentDifferenceVO detailDifference(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentDifferenceStatusVO> listDifferenceStatuses();
    java.util.List<io.mango.payment.api.vo.PaymentDifferenceActionVO> listDifferenceActions();
    io.mango.payment.api.vo.PaymentDifferenceVO handleDifference(
            io.mango.payment.api.command.HandlePaymentDifferenceCommand command);
}
