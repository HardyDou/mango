package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOrderApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderSyncStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentOrderFeignClient", path = "/payment/payment-orders")
public interface PaymentOrderFeignClient extends PaymentOrderApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentOrderVO>> pagePaymentOrders(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentOrderVO> detailPaymentOrder(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentOrderStatusVO>> listPaymentOrderStatuses();

    @Override
    @PostMapping("/sync-status")
    R<PaymentOrderSyncStatusVO> syncPaymentOrderStatus(
            @RequestParam("payOrderNo") String payOrderNo);
}
