package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentOpenApi {

    R<PaymentOpenBusinessOrderVO> createOrder(PaymentOpenRequestCommand command);

    R<PaymentOpenBusinessOrderVO> detailOrder(PaymentOpenRequestCommand command);

    R<PaymentOpenCashierVO> cashier(PaymentOpenRequestCommand command);

    R<PaymentOpenPaymentOrderVO> pay(PaymentOpenRequestCommand command);

    R<PaymentOpenPaymentOrderVO> detailPaymentOrder(PaymentOpenRequestCommand command);

    R<PaymentOpenRefundOrderVO> refund(PaymentOpenRequestCommand command);

    R<PaymentOpenRefundOrderVO> detailRefund(PaymentOpenRequestCommand command);

    R<PaymentOpenReceiptVO> receipt(PaymentOpenRequestCommand command);
}
