package io.mango.payment.core.service;

import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.vo.PaymentOpenBusinessOrderVO;
import io.mango.payment.api.vo.PaymentOpenCashierVO;
import io.mango.payment.api.vo.PaymentOpenPaymentOrderVO;
import io.mango.payment.api.vo.PaymentOpenReceiptVO;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;

public interface IPaymentOpenApiService {

    PaymentOpenBusinessOrderVO createOrder(PaymentOpenRequestCommand command);

    PaymentOpenBusinessOrderVO detailOrder(PaymentOpenRequestCommand command);

    PaymentOpenCashierVO cashier(PaymentOpenRequestCommand command);

    PaymentOpenPaymentOrderVO pay(PaymentOpenRequestCommand command);

    PaymentOpenPaymentOrderVO detailPaymentOrder(PaymentOpenRequestCommand command);

    PaymentOpenRefundOrderVO refund(PaymentOpenRequestCommand command);

    PaymentOpenRefundOrderVO detailRefund(PaymentOpenRequestCommand command);

    PaymentOpenReceiptVO receipt(PaymentOpenRequestCommand command);
}
