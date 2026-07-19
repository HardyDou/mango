package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentCashierApi;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentCashierFeignClient", path = "/payment/cashier")
public interface PaymentCashierFeignClient extends PaymentCashierApi {

    @Override
    @GetMapping("/session")
    R<PaymentCashierSessionVO> detailSession(
            @RequestParam("cashierConfigId") Long cashierConfigId,
            @RequestParam(value = "businessOrderId", required = false) Long businessOrderId);

    @Override
    @PostMapping("/pay")
    R<PaymentCashierPayResultVO> pay(@RequestBody PaymentCashierPayCommand command);

    @Override
    @GetMapping("/pay-result")
    R<PaymentCashierPayResultVO> payResult(
            @RequestParam("payOrderNo") String payOrderNo);

    @Override
    @PostMapping("/pay-result/sync")
    R<PaymentCashierPayResultVO> syncPayResult(
            @RequestParam("payOrderNo") String payOrderNo);

    @Override
    @PostMapping("/offline-collections/transfer-voucher")
    R<PaymentOfflineCollectionVO> submitOfflineTransferVoucher(
            @RequestBody SubmitOfflineTransferVoucherCommand command);
}
