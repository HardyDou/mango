package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;

public interface IPaymentMethodRouteService {

    PageResult<PaymentMethodRouteRuleVO> pageRouteRules(PaymentMethodRoutePageQuery query);

    PaymentMethodRouteRuleVO detailRouteRule(Long id);

    Long createRouteRule(SavePaymentMethodRouteRuleCommand command);

    Boolean updateRouteRule(SavePaymentMethodRouteRuleCommand command);

    Boolean deleteRouteRule(Long id);

    PaymentMethodRouteTrialVO trialRoute(PaymentMethodRouteTrialCommand command);
}
