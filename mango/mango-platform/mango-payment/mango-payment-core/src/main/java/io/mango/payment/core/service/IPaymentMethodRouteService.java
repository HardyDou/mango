package io.mango.payment.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;

public interface IPaymentMethodRouteService {

    R<PageResult<PaymentMethodRouteRuleVO>> pageRouteRules(PaymentMethodRoutePageQuery query);

    R<PaymentMethodRouteRuleVO> detailRouteRule(Long id);

    R<Long> createRouteRule(SavePaymentMethodRouteRuleCommand command);

    R<Boolean> updateRouteRule(SavePaymentMethodRouteRuleCommand command);

    R<Boolean> deleteRouteRule(Long id);

    R<PaymentMethodRouteTrialVO> trialRoute(PaymentMethodRouteTrialCommand command);
}
