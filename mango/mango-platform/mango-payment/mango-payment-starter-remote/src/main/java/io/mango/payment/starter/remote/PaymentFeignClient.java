package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentApi;
import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.RefreshPaymentStatusCommand;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-payment", path = "/payment")
public interface PaymentFeignClient extends PaymentApi {

    @Override
    @PostMapping("/biz-orders")
    R<Long> createBizOrder(@RequestBody CreatePayBizOrderCommand command);

    @Override
    @PostMapping("/payments")
    R<PaymentOrderVO> pay(@RequestBody PayCommand command);

    @Override
    @PostMapping("/biz-orders/close")
    R<Boolean> closeBizOrder(@RequestBody ClosePayBizOrderCommand command);

    @Override
    @PostMapping("/biz-orders/query")
    R<PayBizOrderVO> queryBizOrder(@RequestBody QueryPayBizOrderCommand command);

    @Override
    @PostMapping("/payments/query")
    R<PaymentOrderVO> queryPaymentOrder(@RequestBody QueryPaymentOrderCommand command);

    @Override
    @PostMapping("/payments/refresh")
    R<PaymentOrderVO> refreshPaymentStatus(@RequestBody RefreshPaymentStatusCommand command);

    @Override
    @PostMapping("/payments/notify")
    R<Boolean> paymentNotify(@RequestBody PaymentNotifyCommand command);
}
