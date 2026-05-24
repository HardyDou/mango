package io.mango.payment.core.service;

import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.RefreshPaymentStatusCommand;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;

public interface IPaymentService {

    Long createBizOrder(CreatePayBizOrderCommand command);

    PaymentOrderVO pay(PayCommand command);

    boolean closeBizOrder(ClosePayBizOrderCommand command);

    PayBizOrderVO queryBizOrder(QueryPayBizOrderCommand command);

    PaymentOrderVO queryPaymentOrder(QueryPaymentOrderCommand command);

    PaymentOrderVO refreshPaymentStatus(RefreshPaymentStatusCommand command);

    boolean paymentNotify(PaymentNotifyCommand command);
}
