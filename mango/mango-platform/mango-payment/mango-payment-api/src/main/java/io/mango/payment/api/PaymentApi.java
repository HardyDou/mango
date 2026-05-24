package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.RefreshPaymentStatusCommand;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentApi {

    R<Long> createBizOrder(@Valid CreatePayBizOrderCommand command);

    R<PaymentOrderVO> pay(@Valid PayCommand command);

    R<Boolean> closeBizOrder(@Valid ClosePayBizOrderCommand command);

    R<PayBizOrderVO> queryBizOrder(@Valid QueryPayBizOrderCommand command);

    R<PaymentOrderVO> queryPaymentOrder(@Valid QueryPaymentOrderCommand command);

    R<PaymentOrderVO> refreshPaymentStatus(@Valid RefreshPaymentStatusCommand command);

    R<Boolean> paymentNotify(@Valid PaymentNotifyCommand command);
}
