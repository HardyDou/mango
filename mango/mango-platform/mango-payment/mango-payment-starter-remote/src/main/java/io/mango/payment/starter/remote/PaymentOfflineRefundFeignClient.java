package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOfflineRefundApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineRefundStatusVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentOfflineRefundFeignClient", path = "/payment/offline-refunds")
public interface PaymentOfflineRefundFeignClient extends PaymentOfflineRefundApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentOfflineRefundVO>> pageOfflineRefunds(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentOfflineRefundVO> detailOfflineRefund(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentOfflineRefundStatusVO>> listOfflineRefundStatuses();
}
