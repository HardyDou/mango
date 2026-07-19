package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentReconciliationApi;
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

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentReconciliationFeignClient", path = "/payment/reconciliations")
public interface PaymentReconciliationFeignClient extends PaymentReconciliationApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentReconciliationVO>> pageReconciliations(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentReconciliationVO> detailReconciliation(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentReconciliationStatusVO>> listReconciliationStatuses();

    @Override
    @GetMapping("/bill-fetch-modes")
    R<List<PaymentChannelBillFetchModeVO>> listBillFetchModes();

    @Override
    @PostMapping("/import")
    R<PaymentReconciliationVO> importReconciliation(@RequestBody ImportPaymentReconciliationCommand command);

    @Override
    @PostMapping("/mango-pay/virtual/generate")
    R<PaymentReconciliationVO> generateMangoPayVirtualBill(@RequestBody GenerateMangoPayVirtualBillCommand command);

    @Override
    @PostMapping("/local-order-check/generate")
    R<PaymentReconciliationVO> generateLocalOrderCheck(@RequestBody GeneratePaymentLocalOrderCheckCommand command);

    @Override
    @GetMapping("/bill-sources/page")
    R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/bill-sources/detail")
    R<PaymentChannelBillSourceVO> detailBillSource(@RequestParam("id") Long id);

    @Override
    @GetMapping("/bill-fetch-batches/page")
    R<PageResult<PaymentChannelBillFetchBatchVO>> pageBillFetchBatches(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @PostMapping("/bill-fetch")
    R<PaymentReconciliationVO> fetchChannelBill(@RequestBody FetchPaymentChannelBillCommand command);
}
