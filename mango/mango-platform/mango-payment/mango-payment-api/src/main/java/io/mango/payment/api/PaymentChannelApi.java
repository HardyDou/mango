package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentChannelApi {

    R<PageResult<PaymentChannelVO>> pageChannels(@Valid PaymentConfigPageQuery query);

    R<PaymentChannelVO> detailChannel(@NotNull(message = "通道 ID 不能为空") Long id);

    R<Long> createChannel(@Valid SavePaymentChannelCommand command);

    R<Boolean> updateChannel(@Valid SavePaymentChannelCommand command);

    R<Boolean> deleteChannel(@NotNull(message = "通道 ID 不能为空") Long id);
}
