package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentMethodRouteApi {

    R<PageResult<PaymentMethodRouteRuleVO>> pageRouteRules(@Valid PaymentMethodRoutePageQuery query);

    R<PaymentMethodRouteRuleVO> detailRouteRule(@NotNull(message = "路由规则 ID 不能为空") Long id);

    R<Long> createRouteRule(@Valid SavePaymentMethodRouteRuleCommand command);

    R<Boolean> updateRouteRule(@Valid SavePaymentMethodRouteRuleCommand command);

    R<Boolean> deleteRouteRule(@NotNull(message = "路由规则 ID 不能为空") Long id);

    R<PaymentMethodRouteTrialVO> trialRoute(@Valid PaymentMethodRouteTrialCommand command);
}
