package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.SavePaymentCashierConfigCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentCashierConfigVO;

public interface IPaymentCashierConfigService {

    R<PageResult<PaymentCashierConfigVO>> pageCashierConfigs(PaymentConfigPageQuery query);

    R<PaymentCashierConfigVO> detailCashierConfig(Long id);

    R<Long> createCashierConfig(SavePaymentCashierConfigCommand command);

    R<Boolean> updateCashierConfig(SavePaymentCashierConfigCommand command);

    R<Boolean> deleteCashierConfig(Long id);
}
