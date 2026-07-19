package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentMethodRouteApi;
import io.mango.payment.api.command.PaymentMethodRouteTrialCommand;
import io.mango.payment.api.command.SavePaymentMethodRouteRuleCommand;
import io.mango.payment.api.query.PaymentMethodRoutePageQuery;
import io.mango.payment.api.vo.PaymentMethodRouteRuleVO;
import io.mango.payment.api.vo.PaymentMethodRouteTrialVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentMethodRouteFeignClient", path = "/payment/method-routes")
public interface PaymentMethodRouteFeignClient extends PaymentMethodRouteApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentMethodRouteRuleVO>> pageRouteRules(@SpringQueryMap PaymentMethodRoutePageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentMethodRouteRuleVO> detailRouteRule(@RequestParam("id") Long id);

    @Override
    @PostMapping
    R<Long> createRouteRule(@RequestBody SavePaymentMethodRouteRuleCommand command);

    @Override
    @PutMapping
    R<Boolean> updateRouteRule(@RequestBody SavePaymentMethodRouteRuleCommand command);

    @Override
    @DeleteMapping
    R<Boolean> deleteRouteRule(@RequestParam("id") Long id);

    @Override
    @PostMapping("/trial")
    R<PaymentMethodRouteTrialVO> trialRoute(@RequestBody PaymentMethodRouteTrialCommand command);
}
