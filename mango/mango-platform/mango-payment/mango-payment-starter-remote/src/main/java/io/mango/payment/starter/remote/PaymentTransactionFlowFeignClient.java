package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentTransactionFlowApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentTransactionFlowFeignClient", path = "/payment/transaction-flows")
public interface PaymentTransactionFlowFeignClient extends PaymentTransactionFlowApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentTransactionFlowVO>> pageTransactionFlows(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentTransactionFlowVO> detailTransactionFlow(@RequestParam("id") Long id);
}
