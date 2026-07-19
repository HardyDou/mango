package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentDifferenceApi;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentDifferenceActionVO;
import io.mango.payment.api.vo.PaymentDifferenceStatusVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentDifferenceFeignClient", path = "/payment/differences")
public interface PaymentDifferenceFeignClient extends PaymentDifferenceApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentDifferenceVO>> pageDifferences(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentDifferenceVO> detailDifference(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentDifferenceStatusVO>> listDifferenceStatuses();

    @Override
    @GetMapping("/actions")
    R<List<PaymentDifferenceActionVO>> listDifferenceActions();

    @Override
    @PostMapping("/handle")
    R<PaymentDifferenceVO> handleDifference(@RequestBody HandlePaymentDifferenceCommand command);
}
