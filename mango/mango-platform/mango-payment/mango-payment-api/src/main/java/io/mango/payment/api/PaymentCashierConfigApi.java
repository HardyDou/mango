package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentCashierConfigApi {

    R<PageResult<PaymentCashierConfigVO>> pageCashierConfigs(@Valid PaymentConfigPageQuery query);

    R<PaymentCashierConfigVO> detailCashierConfig(@NotNull(message = "收银台配置 ID 不能为空") Long id);

    R<Long> createCashierConfig(@Valid SavePaymentCashierConfigCommand command);

    R<Boolean> updateCashierConfig(@Valid SavePaymentCashierConfigCommand command);

    R<Boolean> deleteCashierConfig(@NotNull(message = "收银台配置 ID 不能为空") Long id);
}
