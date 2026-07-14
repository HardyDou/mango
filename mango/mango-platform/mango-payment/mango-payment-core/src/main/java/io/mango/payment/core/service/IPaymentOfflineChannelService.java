package io.mango.payment.core.service;

public interface IPaymentOfflineChannelService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentOfflineCollectionVO> pageOfflineCollections(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentOfflineCollectionVO detailOfflineCollection(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO> listOfflineCollectionStatuses();
    io.mango.payment.api.vo.PaymentOfflineCollectionVO submitTransferVoucher(
            io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand command);
    io.mango.payment.api.vo.PaymentOfflineCollectionVO confirmCollection(
            io.mango.payment.api.command.ConfirmOfflineCollectionCommand command);
    io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO importBankStatement(
            io.mango.payment.api.command.ImportOfflineBankStatementCommand command);
    io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO confirmBankStatementMatches(
            io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand command);
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO> pageBankStatementBatches(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO detailBankStatementBatch(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO> listBankStatementBatchStatuses();
    java.util.List<io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO> listBankStatementMatchStatuses();
    io.mango.payment.api.vo.PaymentOfflineRefundVO createOfflineRefund(
            io.mango.payment.api.command.CreateOfflineRefundCommand command);
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentOfflineRefundVO> pageOfflineRefunds(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentOfflineRefundVO detailOfflineRefund(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentOfflineRefundStatusVO> listOfflineRefundStatuses();
}
