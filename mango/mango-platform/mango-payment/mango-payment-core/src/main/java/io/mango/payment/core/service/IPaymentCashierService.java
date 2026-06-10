package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;

public interface IPaymentCashierService {

    R<PaymentCashierSessionVO> detailSession(Long cashierConfigId, Long businessOrderId);

    R<PaymentCashierPayResultVO> pay(PaymentCashierPayCommand command);

    R<PaymentCashierPayResultVO> payResult(String payOrderNo);
}
