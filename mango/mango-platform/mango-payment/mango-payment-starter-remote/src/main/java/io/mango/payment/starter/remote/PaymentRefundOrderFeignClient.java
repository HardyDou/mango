package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentRefundOrderApi;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentRefundOrderFeignClient", path = "/payment/refund-orders")
public interface PaymentRefundOrderFeignClient extends PaymentRefundOrderApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentRefundOrderVO>> pageRefundOrders(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentRefundOrderVO> detailRefundOrder(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentRefundOrderStatusVO>> listRefundOrderStatuses();

    @Override
    @PostMapping("/query-channel")
    R<PaymentRefundOrderVO> queryRefundOrder(@RequestBody QueryPaymentRefundOrderCommand command);
}
