package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand;
import io.mango.payment.api.command.GeneratePaymentLocalOrderCheckCommand;
import io.mango.payment.api.command.ImportPaymentReconciliationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelBillFetchBatchVO;
import io.mango.payment.api.vo.PaymentChannelBillFetchModeVO;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentReconciliationStatusVO;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentReconciliationApi {

    R<PageResult<PaymentReconciliationVO>> pageReconciliations(@Valid PaymentConfigPageQuery query);

    R<PaymentReconciliationVO> detailReconciliation(@NotNull(message = "对账批次 ID 不能为空") Long id);

    R<List<PaymentReconciliationStatusVO>> listReconciliationStatuses();

    R<List<PaymentChannelBillFetchModeVO>> listBillFetchModes();

    R<PaymentReconciliationVO> importReconciliation(@Valid ImportPaymentReconciliationCommand command);

    R<PaymentReconciliationVO> generateMangoPayVirtualBill(@Valid GenerateMangoPayVirtualBillCommand command);

    R<PaymentReconciliationVO> generateLocalOrderCheck(@Valid GeneratePaymentLocalOrderCheckCommand command);

    R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@Valid PaymentConfigPageQuery query);

    R<PaymentChannelBillSourceVO> detailBillSource(@NotNull(message = "账单获取源 ID 不能为空") Long id);

    R<PageResult<PaymentChannelBillFetchBatchVO>> pageBillFetchBatches(@Valid PaymentConfigPageQuery query);

    R<PaymentReconciliationVO> fetchChannelBill(@Valid FetchPaymentChannelBillCommand command);
}
