package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentOpenApi;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-payment", contextId = "paymentOpenFeignClient", path = "/openapi/pay")
public interface PaymentOpenFeignClient extends PaymentOpenApi {

    @Override
    @PostMapping("/orders/create")
    R<PaymentOpenBusinessOrderVO> createOrder(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/orders/detail")
    R<PaymentOpenBusinessOrderVO> detailOrder(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/cashier/detail")
    R<PaymentOpenCashierVO> cashier(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/payments/create")
    R<PaymentOpenPaymentOrderVO> pay(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/payments/detail")
    R<PaymentOpenPaymentOrderVO> detailPaymentOrder(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/refunds/create")
    R<PaymentOpenRefundOrderVO> refund(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/refunds/detail")
    R<PaymentOpenRefundOrderVO> detailRefund(
            @RequestBody PaymentOpenRequestCommand command);

    @Override
    @PostMapping("/receipts/detail")
    R<PaymentOpenReceiptVO> receipt(
            @RequestBody PaymentOpenRequestCommand command);
}
