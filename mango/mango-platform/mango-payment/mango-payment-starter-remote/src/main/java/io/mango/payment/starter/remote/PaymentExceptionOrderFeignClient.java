package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentExceptionOrderApi;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentExceptionOrderActionVO;
import io.mango.payment.api.vo.PaymentExceptionOrderStatusVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentExceptionOrderFeignClient", path = "/payment/exception-orders")
public interface PaymentExceptionOrderFeignClient extends PaymentExceptionOrderApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentExceptionOrderVO>> pageExceptionOrders(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentExceptionOrderVO> detailExceptionOrder(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentExceptionOrderStatusVO>> listExceptionOrderStatuses();

    @Override
    @GetMapping("/actions")
    R<List<PaymentExceptionOrderActionVO>> listExceptionOrderActions();

    @Override
    @PostMapping("/handle")
    R<PaymentExceptionOrderVO> handleExceptionOrder(@RequestBody HandlePaymentExceptionOrderCommand command);
}
