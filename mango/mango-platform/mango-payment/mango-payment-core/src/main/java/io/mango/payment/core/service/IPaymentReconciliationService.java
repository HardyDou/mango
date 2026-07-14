package io.mango.payment.core.service;

public interface IPaymentReconciliationService {
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentReconciliationVO> pageReconciliations(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentReconciliationVO detailReconciliation(Long id);
    java.util.List<io.mango.payment.api.vo.PaymentReconciliationStatusVO> listReconciliationStatuses();
    java.util.List<io.mango.payment.api.vo.PaymentChannelBillFetchModeVO> listBillFetchModes();
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentChannelBillSourceVO> pageBillSources(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentChannelBillSourceVO detailBillSource(Long id);
    io.mango.payment.api.vo.PaymentChannelBillSourceVO saveBillSource(
            io.mango.payment.api.command.SavePaymentChannelBillSourceCommand command);
    io.mango.common.vo.PageResult<io.mango.payment.api.vo.PaymentChannelBillFetchBatchVO> pageBillFetchBatches(
            io.mango.payment.api.query.PaymentConfigPageQuery query);
    io.mango.payment.api.vo.PaymentReconciliationVO fetchChannelBill(
            io.mango.payment.api.command.FetchPaymentChannelBillCommand command);
    io.mango.payment.api.vo.PaymentReconciliationVO importReconciliation(
            io.mango.payment.api.command.ImportPaymentReconciliationCommand command);
    io.mango.payment.api.vo.PaymentReconciliationVO generateMangoPayVirtualBill(
            io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand command);
    io.mango.payment.api.vo.PaymentReconciliationVO generateLocalOrderCheck(
            io.mango.payment.api.command.GeneratePaymentLocalOrderCheckCommand command);
}
