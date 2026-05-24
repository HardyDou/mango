package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.RefundApi;
import io.mango.payment.api.command.QueryRefundOrderCommand;
import io.mango.payment.api.command.RefreshRefundStatusCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.vo.RefundOrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-payment", path = "/payment/refunds")
public interface RefundFeignClient extends RefundApi {

    @Override
    @PostMapping
    R<RefundOrderVO> refund(@RequestBody RefundCommand command);

    @Override
    @PostMapping("/query")
    R<RefundOrderVO> queryRefundOrder(@RequestBody QueryRefundOrderCommand command);

    @Override
    @PostMapping("/refresh")
    R<RefundOrderVO> refreshRefundStatus(@RequestBody RefreshRefundStatusCommand command);
}
