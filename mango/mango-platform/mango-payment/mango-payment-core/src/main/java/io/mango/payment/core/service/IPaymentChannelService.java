package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelVO;

public interface IPaymentChannelService {

    R<PageResult<PaymentChannelVO>> pageChannels(PaymentConfigPageQuery query);

    R<PaymentChannelVO> detailChannel(Long id);

    R<Long> createChannel(SavePaymentChannelCommand command);

    R<Boolean> updateChannel(SavePaymentChannelCommand command);

    R<Boolean> deleteChannel(Long id);

    PageResult<PaymentChannelCapabilityVO> pageChannelCapabilities(PaymentConfigPageQuery query);
}
