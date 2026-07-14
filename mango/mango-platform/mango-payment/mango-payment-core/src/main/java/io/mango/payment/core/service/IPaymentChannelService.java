package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelVO;

public interface IPaymentChannelService {

    PageResult<PaymentChannelVO> pageChannels(PaymentConfigPageQuery query);

    PaymentChannelVO detailChannel(Long id);

    Long createChannel(SavePaymentChannelCommand command);

    Boolean updateChannel(SavePaymentChannelCommand command);

    Boolean deleteChannel(Long id);

    PageResult<PaymentChannelCapabilityVO> pageChannelCapabilities(PaymentConfigPageQuery query);
}
