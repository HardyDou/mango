package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentBusinessOrderApi;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderStatusVO;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentBusinessOrderFeignClient", path = "/payment/business-orders")
public interface PaymentBusinessOrderFeignClient extends PaymentBusinessOrderApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentBusinessOrderVO>> pageBusinessOrders(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentBusinessOrderVO> detailBusinessOrder(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentBusinessOrderStatusVO>> listBusinessOrderStatuses();

    @Override
    @PostMapping
    R<PaymentBusinessOrderVO> createBusinessOrder(@RequestBody CreatePaymentBusinessOrderCommand command);
}
