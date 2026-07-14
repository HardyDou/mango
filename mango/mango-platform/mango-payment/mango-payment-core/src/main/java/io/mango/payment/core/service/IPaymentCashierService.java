package io.mango.payment.core.service;

import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;

public interface IPaymentCashierService {

    PaymentCashierSessionVO detailSession(Long cashierConfigId, Long businessOrderId);

    PaymentCashierPayResultVO pay(PaymentCashierPayCommand command);

    PaymentCashierPayResultVO payResult(String payOrderNo);

    PaymentCashierPayResultVO syncPayResult(String payOrderNo);
}
