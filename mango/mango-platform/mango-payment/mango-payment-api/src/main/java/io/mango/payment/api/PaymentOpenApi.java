package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentOpenApi {

    R<PaymentOpenBusinessOrderVO> createOrder(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenBusinessOrderVO> detailOrder(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenCashierVO> cashier(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenPaymentOrderVO> pay(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenPaymentOrderVO> detailPaymentOrder(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenRefundOrderVO> refund(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenRefundOrderVO> detailRefund(@Valid PaymentOpenRequestCommand command);

    R<PaymentOpenReceiptVO> receipt(@Valid PaymentOpenRequestCommand command);
}
