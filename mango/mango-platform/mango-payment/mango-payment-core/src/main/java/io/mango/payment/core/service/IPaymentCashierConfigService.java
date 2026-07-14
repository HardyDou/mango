package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;

public interface IPaymentCashierConfigService {

    PageResult<PaymentCashierConfigVO> pageCashierConfigs(PaymentConfigPageQuery query);

    PaymentCashierConfigVO detailCashierConfig(Long id);

    Long createCashierConfig(SavePaymentCashierConfigCommand command);

    Boolean updateCashierConfig(SavePaymentCashierConfigCommand command);

    Boolean deleteCashierConfig(Long id);
}
