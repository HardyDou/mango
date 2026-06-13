package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentCashierPayCommand;
import io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand;
import io.mango.payment.api.vo.PaymentCashierPayResultVO;
import io.mango.payment.api.vo.PaymentCashierSessionVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentCashierApi {

    R<PaymentCashierSessionVO> detailSession(@NotNull(message = "收银台配置 ID 不能为空") Long cashierConfigId, Long businessOrderId);

    R<PaymentCashierPayResultVO> pay(@Valid PaymentCashierPayCommand command);

    R<PaymentCashierPayResultVO> payResult(@NotBlank(message = "支付订单号不能为空") String payOrderNo);

    R<PaymentCashierPayResultVO> syncPayResult(@NotBlank(message = "支付订单号不能为空") String payOrderNo);

    R<PaymentOfflineCollectionVO> submitOfflineTransferVoucher(@Valid SubmitOfflineTransferVoucherCommand command);
}
