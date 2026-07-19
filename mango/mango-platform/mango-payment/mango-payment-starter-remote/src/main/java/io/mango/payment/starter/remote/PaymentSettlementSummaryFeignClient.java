package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentSettlementSummaryApi;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentSettlementSummaryFeignClient", path = "/payment/settlement-summaries")
public interface PaymentSettlementSummaryFeignClient extends PaymentSettlementSummaryApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentSettlementSummaryVO>> pageSettlementSummaries(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentSettlementSummaryVO> detailSettlementSummary(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentSettlementSummaryStatusVO>> listSettlementSummaryStatuses();

    @Override
    @PostMapping("/generate")
    R<PaymentSettlementSummaryVO> generateSettlementSummary(@RequestBody GeneratePaymentSettlementSummaryCommand command);

    @Override
    @PostMapping("/confirm")
    R<PaymentSettlementSummaryVO> confirmSettlementSummary(@RequestBody ConfirmPaymentSettlementSummaryCommand command);

    @Override
    @PostMapping("/void")
    R<PaymentSettlementSummaryVO> voidSettlementSummary(@RequestBody VoidPaymentSettlementSummaryCommand command);
}
